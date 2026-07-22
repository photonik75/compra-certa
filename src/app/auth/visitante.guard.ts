import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SessaoService } from './sessao.service';

const ROTA_LISTAS = '/listas';

export const visitanteGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(SessaoService).consultar().pipe(
    map(() => router.createUrlTree([ROTA_LISTAS])),
    catchError(() => of(true)),
  );
};
