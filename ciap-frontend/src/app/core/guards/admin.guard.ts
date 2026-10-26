import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** UI gate for user management. The API still requires the ADMIN role. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasAnyRole('ADMIN')) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
