import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Department queue. The API still limits each head to their own department. */
export const hodGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasAnyRole('HOD', 'ADMIN')) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
