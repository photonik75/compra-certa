import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Categoria, CategoriasService } from '../categorias/categorias.service';

export interface CategoryReference { id: string; name: string; icon: string; available: boolean }
export interface Produto {
  id: string; name: string; category: CategoryReference; defaultUnit: string; active: boolean; version: number;
}
export interface ProductInput { name: string; categoryId: string; defaultUnit: string }
export interface ProductCollection {
  items: Produto[]; page: { nextCursor: string | null; hasMore: boolean };
}
export interface ProductQuery {
  search: string; categoryId: string; status: 'ACTIVE' | 'INACTIVE' | 'ALL'; limit: number;
}

@Injectable({ providedIn: 'root' })
export class ProdutosService {
  private readonly http = inject(HttpClient);
  private readonly categories = inject(CategoriasService);

  listar(query: ProductQuery): Observable<ProductCollection> {
    let params = new HttpParams().set('search', query.search).set('status', query.status).set('limit', query.limit);
    if (query.categoryId) params = params.set('categoryId', query.categoryId);
    return this.http.get<ProductCollection>('/api/v1/products', { params });
  }

  sugerir(search: string): Observable<Produto[]> {
    return this.listar({ search, categoryId: '', status: 'ACTIVE', limit: 10 }).pipe(map((result) => result.items));
  }

  listarCategorias(): Observable<CategoryReference[]> {
    return this.categories.listar({ search: '' }).pipe(map((result) => result.items.map(this.toReference)));
  }

  criar(input: ProductInput): Observable<Produto> {
    const headers = new HttpHeaders().set('Idempotency-Key', crypto.randomUUID());
    return this.http.post<Produto>('/api/v1/products', input, { headers });
  }

  obter(id: string): Observable<Produto> { return this.http.get<Produto>(`/api/v1/products/${id}`); }

  atualizar(id: string, changes: Partial<ProductInput>, version: number): Observable<Produto> {
    return this.http.patch<Produto>(`/api/v1/products/${id}`, changes, { headers: this.version(version) });
  }

  desativar(id: string, version: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/products/${id}`, { headers: this.version(version) });
  }

  private version(value: number): HttpHeaders { return new HttpHeaders().set('If-Match', `"${value}"`); }

  private toReference(category: Categoria): CategoryReference {
    return { id: category.id, name: category.name, icon: category.icon, available: true };
  }
}
