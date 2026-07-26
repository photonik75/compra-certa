import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ListItem, ListaItensService } from '../lista-itens.service';
import { createItemForm, normalizeQuantity } from '../item-form';

@Component({
  selector: 'app-editar-item',
  imports: [ReactiveFormsModule],
  templateUrl: './editar-item.html',
  styleUrl: './editar-item.css',
})
export class EditarItem implements OnInit {
  private readonly service = inject(ListaItensService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly form = createItemForm();
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  readonly itemId = this.route.snapshot.paramMap.get('itemId')!;
  item?: ListItem;
  conflict = false;
  notice = '';

  ngOnInit(): void { this.load(); }

  save(): void {
    if (!this.item || this.form.pristine || this.form.invalid) return;
    const changes: Record<string, string | null> = {};
    for (const key of ['productId', 'quantity', 'unit', 'categoryId', 'notes'] as const) {
      if (this.form.controls[key].dirty) changes[key] = key === 'quantity'
        ? normalizeQuantity(this.form.controls[key].value)
        : this.form.controls[key].value;
    }
    this.service.atualizar(this.listId, this.itemId, changes, this.item.version).subscribe({
      next: () => this.router.navigate(['/listas', this.listId], { fragment: `item-${this.itemId}` }),
      error: (response) => {
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
