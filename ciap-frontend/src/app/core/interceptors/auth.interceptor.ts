import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Attaches the access token to API calls. An expired access token is refreshed
 * once. If the session itself is no longer valid, the user is signed out.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const isAuthCall = req.url.includes('/auth/login')
    || req.url.includes('/auth/refresh')
    || req.url.includes('/auth/logout');
  // A wrong current password is a 401. That is not an expired session.
  const skipRefresh = isAuthCall || req.url.includes('/auth/change-password');

  const send = (token: string | null) => {
    const decorated = (token && !isAuthCall)
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;
    return next(decorated);
  };

  if (!skipRefresh && auth.hasUsableSession() && auth.isAccessExpired()) {
    return auth.refresh().pipe(
      switchMap(() => send(auth.accessToken)),
      catchError(err => {
        auth.expireSession();
        return throwError(() => err);
      })
    );
  }

  return send(auth.accessToken).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401 || skipRefresh) {
        return throwError(() => err);
      }
      if (!auth.hasUsableSession()) {
        auth.expireSession();
        return throwError(() => err);
      }
      return auth.refresh().pipe(
        switchMap(() => send(auth.accessToken)),
        catchError(refreshErr => {
          auth.expireSession();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
