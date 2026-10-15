import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const LOGIN_NOTICE_KEY = 'mv_login_notice';

/**
 * When a signed-in person's session is no longer accepted (for example an administrator blocked the account),
 * sign them out and send them to the login page instead of leaving them on a screen that no longer works.
 */
export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse
        && error.status === 401
        && req.url.includes('/api/')
        && !/\/auth\/(login|register|forgot|logout)\b/.test(req.url)
        && /login is required/i.test(JSON.stringify(error.error ?? ''))
        && auth.token()
      ) {
        auth.clearSession();
        sessionStorage.setItem(LOGIN_NOTICE_KEY, 'Your session has ended. Please sign in again. If your account was blocked, contact the administrator.');
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
