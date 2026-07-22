import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { DadosCadastro } from '../models/dados-cadastro';
import { SessionResponse } from '../models/session-response';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';

export class EmailJaCadastradoError extends Error {}

@Injectable({ providedIn: 'root' })
export class CadastroService {
  private readonly http = inject(HttpClient);

  cadastrar(dados: DadosCadastro): Observable<SessionResponse> {
    return this.http
      .post<SessionResponse>(ENDPOINT_CADASTRO, dados)
      .pipe(
        catchError((erro: HttpErrorResponse) =>
          throwError(() => erro.status === 409 ? new EmailJaCadastradoError() : erro),
        ),
      );
  }
}
