import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** UI gate for institutional screens. The API enforces the same roles. */
export const staffGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'EXPERT')) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
