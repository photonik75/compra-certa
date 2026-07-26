import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ListasService } from '../listas.service';
import {
  LIST_DESCRIPTION_MAX_LENGTH,
  LIST_NAME_DUPLICATE,
  LIST_NAME_MAX_LENGTH,
  LIST_NAME_REQUIRED,
  createListForm,
  normalizeListDescription,
  normalizeListName,
} from '../lista-form';

@Component({
  selector: 'app-nova-lista',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './nova-lista.html',
  styleUrl: './nova-lista.css',
})
export class NovaLista {
  private readonly service = inject(ListasService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly form = createListForm();
  readonly messages = {
    nameRequired: LIST_NAME_REQUIRED,
    nameMaxLength: LIST_NAME_MAX_LENGTH,
    descriptionMaxLength: LIST_DESCRIPTION_MAX_LENGTH,
    nameDuplicate: LIST_NAME_DUPLICATE,
  };
  sending = false;
  submitted = false;
  duplicate = false;
  error = '';

  salvar(): void {
    if (this.sending) return;
    this.submitted = true;
    this.changeDetector.markForCheck();
    this.duplicate = false;
    const name = normalizeListName(this.form.controls.name.value);
    this.form.controls.name.setValue(name);
    if (this.form.invalid) return;
    this.sending = true;
    const description = normalizeListDescription(this.form.controls.description.value);
    this.service.criar({ name, description }).pipe(finalize(() => this.sending = false)).subscribe({
      next: (list) => this.router.navigate(['/listas', list.id]),
      error: (response) => {
        if (response?.error?.code === 'LIST_NAME_ALREADY_IN_USE') this.duplicate = true;
        else this.error = 'Não foi possível criar a lista. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
