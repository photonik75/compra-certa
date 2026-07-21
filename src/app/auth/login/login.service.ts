import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';

export interface DadosLogin {
  email: string;
  password: string;
  manterConectado: boolean;
}

export interface SessionResponse {
  user: { id: string; name: string; email: string; status: string; createdAt: string };
  csrfToken: string;
  expiresAt: string;
}

export class CredenciaisInvalidasError extends Error {}

export class MuitasTentativasError extends Error {}

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly http = inject(HttpClient);

  entrar(dados: DadosLogin): Observable<SessionResponse> {
    return this.http
      .post<SessionResponse>(ENDPOINT_LOGIN, dados)
      .pipe(catchError((erro: HttpErrorResponse) => throwError(() => this.traduzirErro(erro))));
  }

  private traduzirErro(erro: HttpErrorResponse): unknown {
    if (erro.status === 401) return new CredenciaisInvalidasError();
    if (erro.status === 429) return new MuitasTentativasError();
    return erro;
  }
}
