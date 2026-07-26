import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ListStatus = 'ACTIVE' | 'COMPLETED' | 'ALL';
export interface ListSummary { total: number; checked: number; pending: number; percentage: number }
export interface ListCard {
  id: string; name: string; status: string; role: string; updatedAt: string; summary: ListSummary;
}
export interface ListCollection {
  items: ListCard[];
  page: { nextCursor: string | null; hasMore: boolean };
  summary: { activeLists: number; pendingItems: number };
}
export interface ListDetail {
  id: string; name: string; description?: string | null; status: string; role: string; version: number;
}

@Injectable({ providedIn: 'root' })
export class ListasService {
  private readonly http = inject(HttpClient);

  listar(query: { status: ListStatus; search: string; cursor?: string }): Observable<ListCollection> {
    let params = new HttpParams().set('status', query.status).set('search', query.search).set('limit', 30);
    if (query.cursor) params = params.set('cursor', query.cursor);
    return this.http.get<ListCollection>('/api/v1/lists', { params });
  }

  criar(body: { name: string; description: string | null }): Observable<ListDetail> {
    const headers = new HttpHeaders().set('Idempotency-Key', crypto.randomUUID());
    return this.http.post<ListDetail>('/api/v1/lists', body, { headers });
  }

  obter(id: string): Observable<ListDetail> {
    return this.http.get<ListDetail>(`/api/v1/lists/${id}`);
  }

  atualizar(id: string, body: Partial<Pick<ListDetail, 'name' | 'description'>>, version: number):
    Observable<ListDetail> {
    const headers = new HttpHeaders().set('If-Match', `"${version}"`);
    return this.http.patch<ListDetail>(`/api/v1/lists/${id}`, body, { headers });
  }
}
