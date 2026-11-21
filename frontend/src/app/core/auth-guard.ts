import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Api } from './api';

export const authGuard: CanActivateFn = (route, state) => {
  const api = inject(Api);
  const router = inject(Router);
  return api.carregarSessao().pipe(
    map(usuario => {
      if (route.data['admin'] && usuario.papel !== 'ADMIN') {
        router.navigate(['/']);
        return false;
      }
      return true;
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
