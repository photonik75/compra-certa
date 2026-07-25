import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SessionResponse } from './models/session-response';
import { switchMap } from 'rxjs';

export const ENDPOINT_LOGOUT = '/api/v1/auth/sessions/current';
export const ENDPOINT_SESSAO = '/api/v1/auth/session';
const HEADER_CSRF = 'X-CSRF-Token';

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);

  consultar(): Observable<SessionResponse> {
    return this.http.get<SessionResponse>(ENDPOINT_SESSAO);
  }

  sair(): Observable<void> {
  return this.consultar().pipe(
    switchMap(({ csrfToken }) =>
      this.http.delete<void>(ENDPOINT_LOGOUT, {
        headers: { [HEADER_CSRF]: csrfToken },
      }),
    ),
  );
}
}
