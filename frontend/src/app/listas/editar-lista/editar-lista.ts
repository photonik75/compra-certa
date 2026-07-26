import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ListDetail, ListasService } from '../listas.service';
import {
  LIST_DESCRIPTION_MAX_LENGTH,
  LIST_NAME_MAX_LENGTH,
  LIST_NAME_REQUIRED,
  createListForm,
  normalizeListDescription,
  normalizeListName,
} from '../lista-form';

@Component({
  selector: 'app-editar-lista',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './editar-lista.html',
  styleUrl: './editar-lista.css',
})
export class EditarLista implements OnInit {
  private readonly service = inject(ListasService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly form = createListForm();
  readonly messages = {
    nameRequired: LIST_NAME_REQUIRED,
    nameMaxLength: LIST_NAME_MAX_LENGTH,
    descriptionMaxLength: LIST_DESCRIPTION_MAX_LENGTH,
  };
  private list?: ListDetail;
  sending = false;
  message = '';
  conflict = false;

  ngOnInit(): void { this.load(); }

  salvar(): void {
    if (!this.list || this.sending || this.form.invalid || !this.form.dirty) return;
    const changes: Partial<Pick<ListDetail, 'name' | 'description'>> = {};
    if (this.form.controls.name.dirty) changes.name = normalizeListName(this.form.controls.name.value);
    if (this.form.controls.description.dirty) {
      changes.description = normalizeListDescription(this.form.controls.description.value);
    }
    this.sending = true;
    this.service.atualizar(this.list.id, changes, this.list.version).pipe(finalize(() => this.sending = false)).subscribe({
      next: () => this.router.navigate(['/listas', this.list!.id]),
      error: (response) => {
        if (response?.error?.code === 'CONFLICT') {
          this.conflict = true;
          this.message = 'Esta lista foi alterada em outro lugar. Recarregue os dados para continuar.';
        } else this.message = 'Não foi possível salvar as alterações. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  recarregar(): void { this.load(); }

  private load(): void {
    const id = this.route.snapshot.paramMap.get('listId')!;
    this.service.obter(id).subscribe({
      next: (list) => {
        this.list = list;
        this.form.reset({ name: list.name, description: list.description || '' });
        if (list.status === 'COMPLETED') {
          this.message = 'Esta lista está concluída e não pode ser editada.';
          this.form.disable();
        } else if (list.role !== 'OWNER') {
          this.message = 'Lista não encontrada ou indisponível para sua conta.';
          this.form.disable();
        }
        this.changeDetector.markForCheck();
      },
      error: () => this.message = 'Lista não encontrada ou indisponível para sua conta.',
    });
  }
}
