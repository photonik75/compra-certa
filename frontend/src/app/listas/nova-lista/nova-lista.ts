import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ListasService } from '../listas.service';

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
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(60)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(240)] }),
  });
  sending = false;
  submitted = false;
  duplicate = false;
  error = '';

  salvar(): void {
    if (this.sending) return;
    this.submitted = true;
    this.changeDetector.markForCheck();
    this.duplicate = false;
    const name = this.form.controls.name.value.trim().replace(/\s+/g, ' ');
    this.form.controls.name.setValue(name);
    if (this.form.invalid) return;
    this.sending = true;
    const description = this.form.controls.description.value.trim();
    this.service.criar({ name, description: description || null }).pipe(finalize(() => this.sending = false)).subscribe({
      next: (list) => this.router.navigate(['/listas', list.id]),
      error: (response) => {
        if (response?.error?.code === 'LIST_NAME_ALREADY_IN_USE') this.duplicate = true;
        else this.error = 'Não foi possível criar a lista. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
