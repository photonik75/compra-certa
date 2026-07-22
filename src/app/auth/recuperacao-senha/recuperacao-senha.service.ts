import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export const ENDPOINT_RECUPERACAO = '/api/v1/auth/password-reset-requests';

@Injectable({ providedIn: 'root' })
export class RecuperacaoSenhaService {
  private readonly http = inject(HttpClient);

  solicitar(email: string): Observable<void> {
    return this.http.post<void>(ENDPOINT_RECUPERACAO, { email });
  }
}
