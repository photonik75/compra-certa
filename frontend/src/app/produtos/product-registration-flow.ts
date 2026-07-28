import { Produto } from './produtos.service';

export interface ItemDraft {
  quantity: string;
  unit: string;
  categoryId: string;
  notes: string;
}

export interface ProductRegistration {
  returnUrl: string;
  draft: ItemDraft;
}

export interface ProductRegistrationState {
  productRegistration?: ProductRegistration;
  createdProduct?: Produto;
  itemDraft?: ItemDraft;
}
