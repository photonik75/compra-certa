import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

export const ENDPOINT_REDEFINICAO = '/api/v1/auth/password-resets';
const HEADER_IDEMPOTENCIA = 'Idempotency-Key';

interface DadosRedefinicaoSenha {
  token: string;
  newPassword: string;
  passwordConfirmation: string;
}

export class LinkRecuperacaoInvalidoError extends Error {}

@Injectable({ providedIn: 'root' })
export class RedefinicaoSenhaService {
  private readonly http = inject(HttpClient);

  redefinir(dados: DadosRedefinicaoSenha): Observable<void> {
    const headers = new HttpHeaders().set(HEADER_IDEMPOTENCIA, crypto.randomUUID());
    return this.http.post<void>(ENDPOINT_REDEFINICAO, dados, { headers }).pipe(
      catchError((erro: HttpErrorResponse) =>
        throwError(() => erro.status === 400 ? new LinkRecuperacaoInvalidoError() : erro),
      ),
    );
  }
}
