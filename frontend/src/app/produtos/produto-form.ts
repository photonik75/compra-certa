import { FormControl, FormGroup, Validators } from '@angular/forms';

export const PRODUCT_UNITS = [
  ['UNIT', 'unidade'], ['PACKAGE', 'pacote'], ['BOX', 'caixa'], ['BOTTLE', 'garrafa'],
  ['FLASK', 'frasco'], ['CAN', 'lata'], ['BAG', 'saco'], ['TRAY', 'bandeja'], ['DOZEN', 'dúzia'],
  ['KILOGRAM', 'quilograma'], ['GRAM', 'grama'], ['LITER', 'litro'], ['MILLILITER', 'mililitro'],
] as const;
export const PRODUCT_MESSAGES = {
  nameRequired: 'Por favor, informe o nome do produto.',
  nameMaxLength: 'O nome do produto deve ter no máximo 60 caracteres.',
  nameDuplicate: 'Você já possui um produto ativo com este nome.',
  categoryRequired: 'Por favor, escolha uma categoria.',
  categoryUnavailable: 'A categoria selecionada não está mais disponível. Escolha outra categoria.',
  unitRequired: 'Por favor, escolha uma unidade disponível.',
};

export function createProductForm(): FormGroup<{
  name: FormControl<string>;
  categoryId: FormControl<string>;
  defaultUnit: FormControl<string>;
}> {
  return new FormGroup({
    name: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(60)],
    }),
    categoryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    defaultUnit: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(PRODUCT_UNITS.map(([value]) => value).join('|'))],
    }),
  });
}

export function normalizeProductName(value: string): string {
  return value.trim().replace(/\s+/g, ' ');
}
