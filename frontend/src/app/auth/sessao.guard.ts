import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SessaoService } from './sessao.service';

const ROTA_ENTRAR = '/entrar';

export const sessaoGuard: CanActivateFn = (_rota, estado) => {
  const router = inject(Router);
  return inject(SessaoService).consultar().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree([ROTA_ENTRAR], { queryParams: { returnUrl: estado.url } }))),
  );
};
