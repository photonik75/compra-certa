import { FormControl, FormGroup, Validators } from '@angular/forms';

export const CATEGORY_ICONS = ['🥬', '🛍️', '🧃', '🧴', '🍞', '❄️', '🐾', '🛒'] as const;
export const CATEGORY_MESSAGES = {
  nameRequired: 'Por favor, informe o nome da categoria.',
  nameMaxLength: 'O nome da categoria deve ter no máximo 40 caracteres.',
  nameDuplicate: 'Você já possui uma categoria com este nome.',
  iconInvalid: 'Por favor, escolha um ícone disponível.',
};

export function createCategoryForm(): FormGroup<{
  name: FormControl<string>;
  icon: FormControl<string>;
}> {
  return new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(40)],
    }),
    icon: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(CATEGORY_ICONS.join('|'))],
    }),
  });
}

export function normalizeCategoryName(value: string): string {
  return value.trim().replace(/\s+/g, ' ');
}
