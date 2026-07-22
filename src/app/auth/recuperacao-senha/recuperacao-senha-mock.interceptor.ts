import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of, throwError } from 'rxjs';
import { ENDPOINT_RECUPERACAO } from './recuperacao-senha.service';

export const EMAIL_RECUPERACAO_MOCK = 'maria@example.com';

export const recuperacaoSenhaMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_RECUPERACAO) return next(request);
  const email = (request.body as { email: string }).email;
  if (email !== EMAIL_RECUPERACAO_MOCK) {
    return throwError(
      () => new HttpErrorResponse({ status: 503, statusText: 'Service Unavailable', url: request.url }),
    );
  }
  return of(new HttpResponse<void>({ status: 202, statusText: 'Accepted' })).pipe(delay(500));
};
