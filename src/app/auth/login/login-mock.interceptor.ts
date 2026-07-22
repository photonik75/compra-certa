import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { delay, of } from 'rxjs';
import { criarSessaoMock } from '../mocks/criar-sessao-mock';
import { SessaoMockStore } from '../mocks/sessao-mock.store';
import { DadosLogin } from '../models/dados-login';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';

export const loginMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_LOGIN) return next(request);
  const dados = request.body as DadosLogin;
  const sessao = criarSessaoMock({ name: 'Usuário', email: dados.email });
  inject(SessaoMockStore).salvar(sessao);
  return of(
    new HttpResponse({
      status: 200,
      statusText: 'OK',
      body: sessao,
    }),
  ).pipe(delay(500));
};
