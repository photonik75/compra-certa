import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';
import { criarSessaoMock } from '../mocks/criar-sessao-mock';
import { DadosCadastro } from '../models/dados-cadastro';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';

export const cadastroMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_CADASTRO) return next(request);
  const dados = request.body as DadosCadastro;
  return of(
    new HttpResponse({
      status: 201,
      statusText: 'Created',
      body: criarSessaoMock({ name: dados.name, email: dados.email }),
    }),
  ).pipe(delay(500));
};
