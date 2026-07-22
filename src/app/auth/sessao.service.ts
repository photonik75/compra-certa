import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SessionResponse } from './models/session-response';

const ENDPOINT_LOGOUT = '/api/v1/auth/sessions/current';
const ENDPOINT_SESSAO = '/api/v1/auth/session';

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);

  consultar(): Observable<SessionResponse> {
    return this.http.get<SessionResponse>(ENDPOINT_SESSAO);
  }

  sair(): Observable<void> {
    return this.http.delete<void>(ENDPOINT_LOGOUT);
  }
}
