import { Location } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Produto, ProdutosService } from '../../produtos/produtos.service';
import { ProductRegistrationState } from '../../produtos/product-registration-flow';
import { ITEM_MESSAGES, createItemForm, normalizeQuantity, parseQuantity } from '../item-form';
import { ListItem, ListaItensService } from '../lista-itens.service';

@Component({
  selector: 'app-novo-item',
  imports: [ReactiveFormsModule],
  templateUrl: './novo-item.html',
  styleUrl: './novo-item.css',
})
export class NovoItem {
  private readonly items = inject(ListaItensService);
  private readonly products = inject(ProdutosService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly location = inject(Location);
  readonly form = createItemForm();
  readonly messages = ITEM_MESSAGES;
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  suggestions: Produto[] = [];
  selected?: Produto;
  submitted = false;
  sending = false;
  quantityLimit = false;
  notice = '';
  duplicate?: ListItem;

  constructor() {
    const state = this.location.getState() as ProductRegistrationState;
    if (!state.createdProduct || !state.itemDraft) return;
    this.form.patchValue(state.itemDraft);
    this.selectProduct(state.createdProduct);
  }

  searchProducts(value: string): void {
    if (!value) { this.suggestions = []; return; }
    this.products.sugerir(value).subscribe((items) => {
      this.suggestions = items.slice(0, 10);
      this.changeDetector.markForCheck();
    });
  }

  onProductSearch(event: Event): void { this.searchProducts((event.target as HTMLInputElement).value); }

  selectProduct(product: Produto): void {
    this.selected = product;
    this.form.patchValue({
      productId: product.id, unit: product.defaultUnit, categoryId: product.category.id,
    });
  }

  registerProduct(): void {
    const { quantity, unit, categoryId, notes } = this.form.getRawValue();
    this.router.navigate(['/produtos'], {
      state: {
        productRegistration: {
          returnUrl: `/listas/${this.listId}/itens/novo`,
          draft: { quantity, unit, categoryId, notes },
        },
      },
    });
  }

  save(): void {
    if (this.sending) return;
    this.submitted = true;
    const quantity = parseQuantity(this.form.controls.quantity.value);
    this.quantityLimit = quantity > 999999.99;
    if (this.form.invalid || !this.selected || quantity <= 0 || !Number.isFinite(quantity) || this.quantityLimit) {
      this.changeDetector.markForCheck();
      return;
    }
    this.sending = true;
    this.items.criar(this.listId, {
      productId: this.selected.id,
      quantity: normalizeQuantity(this.form.controls.quantity.value),
      unit: this.form.controls.unit.value,
      categoryId: this.form.controls.categoryId.value,
      notes: this.form.controls.notes.value.trim() || null,
    }).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => this.router.navigate(['/listas', this.listId], { fragment: `item-${result.item.id}` }),
      error: (response) => this.handleError(response),
    });
  }

  cancel(): void { this.router.navigate(['/listas', this.listId]); }
  quantityInvalid(): boolean {
    const quantity = parseQuantity(this.form.controls.quantity.value);
    return this.form.controls.quantity.invalid || !Number.isFinite(quantity) || quantity <= 0;
  }

  cancelDuplicate(): void {
    this.duplicate = undefined;
    this.changeDetector.markForCheck();
  }

  editExisting(): void {
    if (this.duplicate) {
      this.router.navigate(['/listas', this.listId, 'itens', this.duplicate.id, 'editar']);
    }
  }

  merge(): void {
    if (!this.duplicate || !this.selected || this.duplicate.unit !== this.form.controls.unit.value) return;
    this.sending = true;
    this.items.criar(this.listId, {
      productId: this.selected.id,
      quantity: normalizeQuantity(this.form.controls.quantity.value),
      unit: this.form.controls.unit.value,
      categoryId: this.form.controls.categoryId.value,
      notes: this.form.controls.notes.value.trim() || null,
      resolution: 'MERGE',
      targetVersion: this.duplicate.version,
    }).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => this.router.navigate(['/listas', this.listId], {
        fragment: `item-${result.item.id}`,
      }),
      error: () => {
        this.notice = 'Não foi possível somar as quantidades. Recarregue a lista e tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }

  private handleError(response: any): void {
    if (response?.error?.code !== 'DUPLICATE_ITEM') {
      this.notice = 'Não foi possível adicionar o item. Tente novamente em alguns instantes.';
      this.changeDetector.markForCheck();
      return;
    }
    this.items.listar(this.listId).subscribe((collection) => {
      const normalized = this.selected?.name.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
      this.duplicate = collection.items.find((item) =>
        item.product.name.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase() === normalized);
      this.notice = this.duplicate ? '' : 'Este produto já está na lista.';
      this.changeDetector.markForCheck();
    });
  }
}
