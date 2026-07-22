import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { delay, of, throwError } from 'rxjs';
import { SessaoMockStore } from '../mocks/sessao-mock.store';
import { ENDPOINT_REDEFINICAO } from './redefinicao-senha.service';

export const TOKEN_REDEFINICAO_MOCK = 'token-recuperacao';

export const redefinicaoSenhaMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_REDEFINICAO) return next(request);
  const token = (request.body as { token: string }).token;
  if (token !== TOKEN_REDEFINICAO_MOCK) {
    return throwError(
      () => new HttpErrorResponse({ status: 400, statusText: 'Bad Request', url: request.url }),
    );
  }
  inject(SessaoMockStore).limpar();
  return of(new HttpResponse<void>({ status: 204, statusText: 'No Content' })).pipe(delay(500));
};
