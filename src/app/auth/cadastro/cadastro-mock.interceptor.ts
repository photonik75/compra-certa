import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';
import { DadosCadastro } from '../models/dados-cadastro';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';

export const cadastroMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_CADASTRO) return next(request);
  const dados = request.body as DadosCadastro;
  return of(
    new HttpResponse({
      status: 201,
      statusText: 'Created',
      body: {
        user: {
          id: crypto.randomUUID(),
          name: dados.name,
          email: dados.email,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        },
        csrfToken: 'mock-csrf-token',
        expiresAt: new Date(Date.now() + 86_400_000).toISOString(),
      },
    }),
  ).pipe(delay(500));
};
