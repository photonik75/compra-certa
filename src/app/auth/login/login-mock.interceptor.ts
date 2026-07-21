import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';

export const loginMockInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.method !== 'POST' || request.url !== ENDPOINT_LOGIN) return next(request);
  const dados = request.body as { email: string };
  return of(
    new HttpResponse({
      status: 200,
      statusText: 'OK',
      body: {
        user: {
          id: crypto.randomUUID(),
          name: 'Usuário',
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
