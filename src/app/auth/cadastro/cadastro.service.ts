import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';

export interface DadosCadastro {
  name: string;
  email: string;
  password: string;
  passwordConfirmation: string;
}

export interface SessionResponse {
  user: { id: string; name: string; email: string; status: string; createdAt: string };
  csrfToken: string;
  expiresAt: string;
}

export class EmailJaCadastradoError extends Error {}

@Injectable({ providedIn: 'root' })
export class CadastroService {
  private readonly http = inject(HttpClient);

  cadastrar(dados: DadosCadastro): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(ENDPOINT_CADASTRO, dados).pipe(catchError((erro: HttpErrorResponse) => throwError(() => erro.status === 409 ? new EmailJaCadastradoError() : erro)));
  }
}
