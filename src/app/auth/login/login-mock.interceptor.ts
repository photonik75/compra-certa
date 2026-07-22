import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';
import { criarSessaoMock } from '../mocks/criar-sessao-mock';
import { DadosLogin } from '../models/dados-login';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';

export const loginMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_LOGIN) return next(request);
  const dados = request.body as DadosLogin;
  return of(
    new HttpResponse({
      status: 200,
      statusText: 'OK',
      body: criarSessaoMock({ name: 'Usuário', email: dados.email }),
    }),
  ).pipe(delay(500));
};
