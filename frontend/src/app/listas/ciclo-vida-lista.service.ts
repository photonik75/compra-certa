import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { mutationHeaders } from '../shared/mutation-headers';

@Injectable({ providedIn: 'root' })
export class CicloVidaListaService {
  private readonly http = inject(HttpClient);

  alterarStatus(listId: string, status: 'ACTIVE' | 'COMPLETED', version: number): Observable<any> {
    return this.http.put(`/api/v1/lists/${listId}/status`, { status }, {
      headers: mutationHeaders(version),
    });
  }

  excluir(listId: string, version: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/lists/${listId}`, { headers: mutationHeaders(version) });
  }
}
