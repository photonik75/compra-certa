import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Categoria {
  id: string;
  name: string;
  icon: string;
  activeProductCount: number;
  version: number;
}

export interface CategoryCollection {
  items: Categoria[];
  page: { nextCursor: string | null; hasMore: boolean };
}

export interface CategoryInput {
  name: string;
  icon: string;
}

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly http = inject(HttpClient);

  listar(query: { search: string }): Observable<CategoryCollection> {
    const params = new HttpParams().set('search', query.search).set('limit', 30);
    return this.http.get<CategoryCollection>('/api/v1/categories', { params });
  }

  criar(input: CategoryInput): Observable<Categoria> {
    const headers = new HttpHeaders().set('Idempotency-Key', crypto.randomUUID());
    return this.http.post<Categoria>('/api/v1/categories', input, { headers });
  }

  obter(id: string): Observable<Categoria> {
    return this.http.get<Categoria>(`/api/v1/categories/${id}`);
  }

  atualizar(id: string, changes: Partial<CategoryInput>, version: number): Observable<Categoria> {
    const headers = this.versionHeaders(version);
    return this.http.patch<Categoria>(`/api/v1/categories/${id}`, changes, { headers });
  }

  excluir(id: string, version: number): Observable<void> {
    const headers = this.versionHeaders(version);
    return this.http.delete<void>(`/api/v1/categories/${id}`, { headers });
  }

  private versionHeaders(version: number): HttpHeaders {
    return new HttpHeaders().set('If-Match', `"${version}"`);
  }
}
