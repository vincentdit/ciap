import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.hasUsableSession()) {
    return true;
  }
  if (auth.isAuthenticated() || auth.sessionJustExpired()) {
    auth.expireSession();
    return router.createUrlTree(['/login'], { queryParams: { expired: '1' } });
  }
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};
