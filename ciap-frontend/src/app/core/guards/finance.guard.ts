import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Finance screens. The API enforces FINANCE or ADMIN on its own. */
export const financeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasAnyRole('FINANCE', 'ADMIN')) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
