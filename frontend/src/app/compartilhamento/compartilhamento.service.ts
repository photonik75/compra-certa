import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { mutationHeaders } from '../shared/mutation-headers';

@Injectable({ providedIn: 'root' })
export class CompartilhamentoService {
  private readonly http = inject(HttpClient);
  consultarAcesso(listId: string): Observable<any> {
    return this.http.get(`/api/v1/lists/${listId}/access`);
  }
  convidar(listId: string, email: string): Observable<any> {
    return this.http.post(`/api/v1/lists/${listId}/invitations`, { email }, { headers: mutationHeaders() });
  }
  reenviar(listId: string, invitationId: string, version: number): Observable<any> {
    return this.http.post(`/api/v1/lists/${listId}/invitations/${invitationId}/resend`, {}, {
      headers: mutationHeaders(version),
    });
  }
  cancelarConvite(listId: string, invitationId: string, version: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/lists/${listId}/invitations/${invitationId}`, {
      headers: mutationHeaders(version),
    });
  }
  preview(token: string): Observable<any> {
    return this.http.post('/api/v1/invitations/preview', { token });
  }
  aceitar(token: string): Observable<any> {
    return this.http.post('/api/v1/invitations/accept', { token }, { headers: mutationHeaders() });
  }
  removerMembro(listId: string, userId: string, version: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/lists/${listId}/members/${userId}`, {
      headers: mutationHeaders(version),
    });
  }
  sair(listId: string, version: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/lists/${listId}/members/me`, { headers: mutationHeaders(version) });
  }
}
