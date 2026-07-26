import { HttpHeaders } from '@angular/common/http';

export function mutationHeaders(version?: number): HttpHeaders {
  let headers = new HttpHeaders().set('Idempotency-Key', crypto.randomUUID());
  if (version !== undefined) headers = headers.set('If-Match', `"${version}"`);
  return headers;
}
