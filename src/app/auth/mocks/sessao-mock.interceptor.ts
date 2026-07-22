import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ENDPOINT_LOGOUT, ENDPOINT_SESSAO } from '../sessao.service';
import { SessaoMockStore } from './sessao-mock.store';

export const sessaoMockInterceptor: HttpInterceptorFn = (request, next) => {
  const store = inject(SessaoMockStore);
  if (request.method === 'GET' && request.url === ENDPOINT_SESSAO) {
    const sessao = store.obter();
    if (!sessao) return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));
    return of(new HttpResponse({ status: 200, statusText: 'OK', body: sessao }));
  }
  if (request.method === 'DELETE' && request.url === ENDPOINT_LOGOUT) {
    store.limpar();
    return of(new HttpResponse<void>({ status: 204, statusText: 'No Content' }));
  }
  return next(request);
};
