import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { SessionResponse } from './models/session-response';

const SESSION_ENDPOINT = '/api/v1/auth/session';
const CSRF_HEADER = 'X-CSRF-Token';
const PROTECTED_RESOURCES = [
  '/api/v1/categories',
  '/api/v1/invitations',
  '/api/v1/lists',
  '/api/v1/products',
];

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  const mutation = request.method !== 'GET' && request.method !== 'HEAD';
  const protectedResource = PROTECTED_RESOURCES.some((resource) => request.url.startsWith(resource));
  if (!mutation || !protectedResource || request.headers.has(CSRF_HEADER)) return next(request);
  return inject(HttpClient).get<SessionResponse>(SESSION_ENDPOINT).pipe(
    switchMap(({ csrfToken }) => next(request.clone({
      headers: request.headers.set(CSRF_HEADER, csrfToken),
    }))),
  );
};
