import { FormControl, FormGroup, Validators } from '@angular/forms';
import { PRODUCT_UNITS } from '../produtos/produto-form';

export const ITEM_MESSAGES = {
  product: 'Selecione um produto válido na lista de sugestões.',
  quantity: 'Informe uma quantidade maior que zero.',
  quantityMax: 'A quantidade deve ser menor ou igual a 999999,99.',
  notesMax: 'A observação deve ter no máximo 240 caracteres.',
};

export function createItemForm() {
  return new FormGroup({
    productId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    quantity: new FormControl('', { nonNullable: true, validators: Validators.required }),
    unit: new FormControl('', { nonNullable: true, validators: Validators.required }),
    categoryId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(240) }),
  });
}

export function parseQuantity(value: string): number {
  return Number(value.replace(',', '.'));
}

export function normalizeQuantity(value: string): string {
  return String(parseQuantity(value));
}

export function formatQuantity(value: string): string {
  return Number(value).toLocaleString('pt-BR', { maximumFractionDigits: 2 });
}

export function unitLabel(value: string): string {
  return PRODUCT_UNITS.find(([unit]) => unit === value)?.[1] ?? value;
}
