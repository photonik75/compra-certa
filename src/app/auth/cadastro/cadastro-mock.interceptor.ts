import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { delay, of } from 'rxjs';
import { criarSessaoMock } from '../mocks/criar-sessao-mock';
import { SessaoMockStore } from '../mocks/sessao-mock.store';
import { DadosCadastro } from '../models/dados-cadastro';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';

export const cadastroMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_CADASTRO) return next(request);
  const dados = request.body as DadosCadastro;
  const sessao = criarSessaoMock({ name: dados.name, email: dados.email });
  inject(SessaoMockStore).salvar(sessao);
  return of(
    new HttpResponse({
      status: 201,
      statusText: 'Created',
      body: sessao,
    }),
  ).pipe(delay(500));
};
