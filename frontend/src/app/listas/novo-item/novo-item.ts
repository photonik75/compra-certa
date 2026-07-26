import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Produto, ProdutosService } from '../../produtos/produtos.service';
import { ITEM_MESSAGES, createItemForm, normalizeQuantity, parseQuantity } from '../item-form';
import { ListaItensService } from '../lista-itens.service';

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
  readonly form = createItemForm();
  readonly messages = ITEM_MESSAGES;
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  suggestions: Produto[] = [];
  selected?: Produto;
  submitted = false;
  sending = false;
  quantityLimit = false;
  notice = '';

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
      error: (response) => {
        this.notice = response?.error?.code === 'DUPLICATE_ITEM'
          ? 'Este produto já está na lista. Edite o item existente ou some as quantidades.'
          : 'Não foi possível adicionar o item. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  cancel(): void { this.router.navigate(['/listas', this.listId]); }
}
