import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { homePath, ROLES } from '../shared/format';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.loggedIn() ? true : router.createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.loggedIn() ? router.createUrlTree([homePath(auth.user()?.role)]) : true;
};

export function roleGuard(roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.user()?.role || '';
    return roles.includes(role) ? true : router.createUrlTree([homePath(role)]);
  };
}

export const memberGuard: CanActivateFn = roleGuard([ROLES.USER]);
export const adminGuard: CanActivateFn = roleGuard([ROLES.ADMIN, ROLES.SUPERADMIN]);
export const superGuard: CanActivateFn = roleGuard([ROLES.SUPERADMIN]);
