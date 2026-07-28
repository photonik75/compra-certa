import { Location } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  PRODUCT_MESSAGES, PRODUCT_UNITS, createProductForm, normalizeProductName,
} from './produto-form';
import { CategoryReference, Produto, ProductInput, ProdutosService } from './produtos.service';
import { ProductRegistration, ProductRegistrationState } from './product-registration-flow';

type Dialog = 'create' | 'edit' | 'deactivate' | null;

@Component({
  selector: 'app-produtos',
  imports: [ReactiveFormsModule],
  templateUrl: './produtos.html',
  styleUrl: './produtos.css',
})
export class Produtos implements OnInit {
  private readonly service = inject(ProdutosService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  readonly form = createProductForm();
  readonly units = PRODUCT_UNITS;
  readonly messages = PRODUCT_MESSAGES;
  products: Produto[] = [];
  categories: CategoryReference[] = [];
  dialog: Dialog = null;
  selected?: Produto;
  query = '';
  categoryId = '';
  submitted = false;
  sending = false;
  duplicate = false;
  conflict = false;
  notice = '';
  private trigger?: HTMLElement;
  private productRegistration?: ProductRegistration;

  ngOnInit(): void {
    this.productRegistration =
      (this.location.getState() as ProductRegistrationState).productRegistration;
    forkJoin({ categories: this.service.listarCategorias(), products: this.fetch() }).subscribe({
      next: ({ categories, products }) => {
        this.categories = categories.filter((category) => category.available);
        this.products = this.sort(products.items);
        if (this.productRegistration) this.openCreate();
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  search(value: string): void { this.query = normalizeProductName(value).slice(0, 60); this.load(); }
  onSearch(event: Event): void { this.search((event.target as HTMLInputElement).value); }
  selectCategory(value: string): void { this.categoryId = value; this.load(); }
  onCategory(event: Event): void { this.selectCategory((event.target as HTMLSelectElement).value); }
  clearSearch(): void { this.query = ''; this.load(); }
  clearFilters(): void { this.query = ''; this.categoryId = ''; this.load(); }

  openCreate(source?: Event | HTMLElement): void {
    this.prepare('create', undefined, source);
    this.form.reset({ name: '', categoryId: '', defaultUnit: '' });
  }

  openEdit(product: Produto, source?: Event | HTMLElement): void {
    this.prepare('edit', product, source);
    this.form.reset({
      name: product.name, categoryId: product.category.id, defaultUnit: product.defaultUnit,
    });
  }

  openDeactivate(product: Produto, source?: Event | HTMLElement): void {
    this.prepare('deactivate', product, source);
  }

  save(): void {
    if (this.sending || !this.dialog || this.dialog === 'deactivate') return;
    this.submitted = true;
    this.duplicate = false;
    const name = normalizeProductName(this.form.controls.name.value);
    this.form.controls.name.setValue(name);
    if (this.form.invalid) { this.changeDetector.markForCheck(); return; }
    const input = {
      name, categoryId: this.form.controls.categoryId.value,
      defaultUnit: this.form.controls.defaultUnit.value,
    };
    this.sending = true;
    if (this.dialog === 'create') this.create(input);
    else this.update(input);
  }

  confirmDeactivate(): void {
    if (!this.selected || this.sending) return;
    const product = this.selected;
    this.sending = true;
    this.service.desativar(product.id, product.version).pipe(finalize(() => this.sending = false)).subscribe({
      next: () => {
        this.products = this.products.filter((item) => item.id !== product.id);
        this.close();
        this.notice = 'Produto desativado com sucesso.';
      },
      error: () => {
        this.notice = 'Não foi possível desativar o produto. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  reload(): void {
    if (!this.selected) return;
    this.service.obter(this.selected.id).subscribe((product) => this.openEdit(product, this.trigger));
  }

  close(): void {
    this.dialog = null;
    this.selected = undefined;
    this.submitted = false;
    this.conflict = false;
    this.changeDetector.markForCheck();
    queueMicrotask(() => this.trigger?.focus());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void { if (this.dialog) this.close(); }

  unitLabel(value: string): string {
    return this.units.find(([unit]) => unit === value)?.[1] ?? value;
  }

  private fetch() {
    return this.service.listar({
      search: this.query, categoryId: this.categoryId, status: 'ACTIVE', limit: 30,
    });
  }

  private load(): void {
    this.fetch().subscribe({
      next: (result) => { this.products = this.sort(result.items); this.changeDetector.markForCheck(); },
      error: () => undefined,
    });
  }

  private create(input: ProductInput): void {
    this.service.criar(input).pipe(finalize(() => this.sending = false)).subscribe({
      next: (product) => {
        if (this.productRegistration) {
          const { returnUrl, draft } = this.productRegistration;
          this.router.navigateByUrl(returnUrl, {
            state: { createdProduct: product, itemDraft: draft },
          });
          return;
        }
        this.products = this.sort([...this.products, product]);
        this.close();
        this.notice = 'Produto criado com sucesso.';
      },
      error: (response) => this.handleError(response, 'Não foi possível criar o produto. '
        + 'Tente novamente em alguns instantes.'),
    });
  }

  private update(input: ProductInput): void {
    if (!this.selected || this.form.pristine) { this.sending = false; return; }
    const changes: Partial<ProductInput> = {};
    if (this.form.controls.name.dirty) changes.name = input.name;
    if (this.form.controls.categoryId.dirty) changes.categoryId = input.categoryId;
    if (this.form.controls.defaultUnit.dirty) changes.defaultUnit = input.defaultUnit;
    this.service.atualizar(this.selected.id, changes, this.selected.version)
      .pipe(finalize(() => this.sending = false)).subscribe({
        next: (product) => {
          this.products = this.sort(this.products.map((item) => item.id === product.id ? product : item));
          this.close();
          this.notice = 'Produto atualizado com sucesso.';
        },
        error: (response) => this.handleError(response, 'Não foi possível atualizar o produto. '
          + 'Tente novamente em alguns instantes.'),
      });
  }

  private handleError(response: { error?: { code?: string } }, fallback: string): void {
    const code = response?.error?.code;
    if (code === 'PRODUCT_NAME_ALREADY_IN_USE') this.duplicate = true;
    else if (code === 'CATEGORY_UNAVAILABLE') this.notice = this.messages.categoryUnavailable;
    else if (code === 'CONFLICT') {
      this.conflict = true;
      this.notice = 'Este produto foi alterado em outro lugar. Recarregue os dados para continuar.';
    } else this.notice = fallback;
    this.changeDetector.markForCheck();
  }

  private prepare(dialog: Dialog, product?: Produto, source?: Event | HTMLElement): void {
    this.dialog = dialog;
    this.selected = product;
    this.trigger = source instanceof Event ? source.currentTarget as HTMLElement : source;
    this.notice = '';
    this.duplicate = false;
    this.conflict = false;
    this.submitted = false;
    this.changeDetector.markForCheck();
  }

  private sort(products: Produto[]): Produto[] {
    return products.map((product, index) => ({ product, index }))
      .sort((a, b) => a.product.name.localeCompare(b.product.name, 'pt-BR') || a.index - b.index)
      .map(({ product }) => product);
  }
}
