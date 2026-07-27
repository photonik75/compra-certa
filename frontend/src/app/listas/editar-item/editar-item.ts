import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ListItem, ListaItensService } from '../lista-itens.service';
import { createItemForm, normalizeQuantity } from '../item-form';
import { Produto, ProdutosService } from '../../produtos/produtos.service';

@Component({
  selector: 'app-editar-item',
  imports: [ReactiveFormsModule],
  templateUrl: './editar-item.html',
  styleUrl: './editar-item.css',
})
export class EditarItem implements OnInit {
  private readonly service = inject(ListaItensService);
  private readonly products = inject(ProdutosService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly form = createItemForm();
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  readonly itemId = this.route.snapshot.paramMap.get('itemId')!;
  item?: ListItem;
  conflict = false;
  notice = '';
  suggestions: Produto[] = [];
  duplicate?: ListItem;

  ngOnInit(): void { this.load(); }

  save(): void {
    if (!this.item || this.form.pristine || this.form.invalid) return;
    const changes: Record<string, string | number | null> = {};
    for (const key of ['productId', 'quantity', 'unit', 'categoryId', 'notes'] as const) {
      if (this.form.controls[key].dirty) changes[key] = key === 'quantity'
        ? normalizeQuantity(this.form.controls[key].value)
        : this.form.controls[key].value;
    }
    this.service.atualizar(this.listId, this.itemId, changes, this.item.version).subscribe({
      next: () => this.router.navigate(['/listas', this.listId], { fragment: `item-${this.itemId}` }),
      error: (response) => {
        if (response?.error?.code === 'DUPLICATE_ITEM') {
          this.service.listar(this.listId).subscribe((collection) => {
            const productId = this.form.controls.productId.value;
            this.duplicate = collection.items.find((candidate) =>
              candidate.id !== this.itemId && candidate.product.id === productId);
            this.changeDetector.markForCheck();
          });
          return;
        }
        this.conflict = response?.error?.code === 'CONFLICT';
        this.notice = this.conflict
          ? 'Este item foi alterado em outro lugar. Recarregue os dados para continuar.'
          : 'Não foi possível atualizar o item. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  reload(): void { this.load(); }
  cancel(): void { this.router.navigate(['/listas', this.listId]); }
  searchProducts(value: string): void {
    if (!value) { this.suggestions = []; return; }
    this.products.sugerir(value).subscribe((items) => {
      this.suggestions = items.slice(0, 10);
      this.changeDetector.markForCheck();
    });
  }
  onProductSearch(event: Event): void {
    this.searchProducts((event.target as HTMLInputElement).value);
  }
  selectProduct(product: Produto): void {
    this.form.patchValue({
      productId: product.id,
      unit: product.defaultUnit,
      categoryId: product.category.id,
    });
    this.form.controls.productId.markAsDirty();
    this.form.controls.unit.markAsDirty();
    this.form.controls.categoryId.markAsDirty();
    this.suggestions = [];
  }
  cancelDuplicate(): void { this.duplicate = undefined; this.changeDetector.markForCheck(); }
  merge(): void {
    if (!this.item || !this.duplicate || this.duplicate.unit !== this.form.controls.unit.value) return;
    const changes: any = {
      productId: this.form.controls.productId.value,
      quantity: normalizeQuantity(this.form.controls.quantity.value),
      unit: this.form.controls.unit.value,
      categoryId: this.form.controls.categoryId.value,
      notes: this.form.controls.notes.value || null,
      resolution: 'MERGE',
      targetVersion: this.duplicate.version,
    };
    this.service.atualizar(this.listId, this.itemId, changes, this.item.version).subscribe({
      next: (result) => this.router.navigate(['/listas', this.listId], {
        fragment: `item-${result.item.id}`,
      }),
      error: () => {
        this.notice = 'Não foi possível somar os itens. Recarregue os dados e tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }

  private load(): void {
    this.service.obter(this.listId, this.itemId).subscribe({
      next: (item) => {
        this.item = item;
        this.form.reset({
          productId: item.product.id, quantity: item.quantity, unit: item.unit,
          categoryId: item.category.id, notes: item.notes ?? '',
        });
        this.changeDetector.markForCheck();
      },
      error: () => this.notice = 'Item não encontrado ou indisponível.',
    });
  }
}
