import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ListItem {
  id: string;
  product: { id: string; name: string };
  category: { id: string; name: string; icon: string };
  quantity: string;
  unit: string;
  notes?: string | null;
  checked: boolean;
  checkedBy?: { name: string } | null;
  checkedAt?: string | null;
  version: number;
}
export interface ListSummary { total: number; checked: number; pending: number; percentage: number }
export interface ItemCollection {
  items: ListItem[];
  page: { nextCursor: string | null; hasMore: boolean };
  listSummary: ListSummary;
  listVersion: number;
}
export interface ItemInput {
  productId: string; quantity: string; unit: string; categoryId: string; notes: string | null;
}

@Injectable({ providedIn: 'root' })
export class ListaItensService {
  private readonly http = inject(HttpClient);

  listar(listId: string): Observable<ItemCollection> {
    return this.http.get<ItemCollection>(`/api/v1/lists/${listId}/items`, {
      params: new HttpParams().set('limit', 30),
    });
  }

  criar(listId: string, input: ItemInput): Observable<any> {
    return this.http.post(`/api/v1/lists/${listId}/items`, input, { headers: this.idempotency() });
  }

  obter(listId: string, itemId: string): Observable<ListItem> {
    return this.http.get<ListItem>(`/api/v1/lists/${listId}/items/${itemId}`);
  }

  atualizar(listId: string, itemId: string, changes: Partial<ItemInput>, version: number): Observable<any> {
    return this.http.patch(`/api/v1/lists/${listId}/items/${itemId}`, changes, {
      headers: this.mutationHeaders(version),
    });
  }

  remover(listId: string, itemId: string, version: number): Observable<any> {
    return this.http.delete(`/api/v1/lists/${listId}/items/${itemId}`, {
      headers: this.mutationHeaders(version),
    });
  }

  marcar(listId: string, itemId: string, checked: boolean, version: number): Observable<any> {
    return this.http.put(`/api/v1/lists/${listId}/items/${itemId}/checked`, { checked }, {
      headers: new HttpHeaders().set('If-Match', `"${version}"`),
    });
  }

  private idempotency(): HttpHeaders {
    return new HttpHeaders().set('Idempotency-Key', crypto.randomUUID());
  }

  private mutationHeaders(version: number): HttpHeaders {
    return this.idempotency().set('If-Match', `"${version}"`);
  }
}
