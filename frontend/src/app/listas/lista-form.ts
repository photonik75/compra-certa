import { FormControl, FormGroup, Validators } from '@angular/forms';

export const LIST_NAME_REQUIRED = 'Por favor, informe o nome da lista.';
export const LIST_NAME_MAX_LENGTH = 'O nome da lista deve ter no máximo 60 caracteres.';
export const LIST_DESCRIPTION_MAX_LENGTH = 'A descrição deve ter no máximo 240 caracteres.';
export const LIST_NAME_DUPLICATE = 'Você já possui uma lista com este nome.';

export interface ListFormValue {
  name: string;
  description: string;
}

export function createListForm(): FormGroup<{
  name: FormControl<string>;
  description: FormControl<string>;
}> {
  return new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(60)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(240)],
    }),
  });
}

export function normalizeListName(value: string): string {
  return value.trim().replace(/\s+/g, ' ');
}

export function normalizeListDescription(value: string): string | null {
  return value.trim() || null;
}
