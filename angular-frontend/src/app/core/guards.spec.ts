import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { adminGuard, authGuard, guestGuard, memberGuard, roleGuard, superGuard } from './guards';
import { ROLES } from './format';

describe('route guards', () => {
  const router = { createUrlTree: jasmine.createSpy('createUrlTree').and.callFake((commands: unknown[]) => commands as unknown as UrlTree) } as unknown as Router;
  let logged = false;
  let currentRole = '';
  const auth = {
    loggedIn: () => logged,
    user: () => currentRole ? { role: currentRole } : null,
  } as unknown as AuthService;

  beforeEach(() => {
    logged = false;
    currentRole = '';
    router.createUrlTree = jasmine.createSpy('createUrlTree').and.callFake((commands: unknown[]) => commands as unknown as UrlTree);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('allows authenticated users and redirects guests', () => {
    logged = true;
    expect(TestBed.runInInjectionContext(() => authGuard({} as never, {} as never))).toBeTrue();
    logged = false;
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBeTruthy();
  });

  it('redirects logged-in users away from guest routes', () => {
    logged = true;
    currentRole = ROLES.ADMIN;
    expect(TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never))).toBeTruthy();
    expect(router.createUrlTree).toHaveBeenCalledWith(['/admin']);
    logged = false;
    expect(TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never))).toBeTrue();
  });

  it('allows and rejects configured roles', () => {
    currentRole = ROLES.USER;
    expect(TestBed.runInInjectionContext(() => roleGuard([ROLES.USER])({} as never, {} as never))).toBeTrue();
    expect(TestBed.runInInjectionContext(() => roleGuard([ROLES.ADMIN])({} as never, {} as never))).toBeTruthy();
    expect(TestBed.runInInjectionContext(() => memberGuard({} as never, {} as never))).toBeTrue();
    expect(TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never))).toBeTruthy();
    expect(TestBed.runInInjectionContext(() => superGuard({} as never, {} as never))).toBeTruthy();
  });

  it('allows admin and superadmin through admin guard', () => {
    currentRole = ROLES.ADMIN;
    expect(TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never))).toBeTrue();
    currentRole = ROLES.SUPERADMIN;
    expect(TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never))).toBeTrue();
    expect(TestBed.runInInjectionContext(() => superGuard({} as never, {} as never))).toBeTrue();
  });
});
