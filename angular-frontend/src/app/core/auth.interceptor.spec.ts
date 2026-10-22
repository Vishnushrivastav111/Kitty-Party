import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    localStorage.clear();
  });

  it('passes requests without a token unchanged', () => {
    http.get('/api/users').subscribe();
    const req = controller.expectOne('/api/users');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('adds bearer token to API requests', () => {
    localStorage.setItem('mv_token', 'abc123');
    http.get('/api/users').subscribe();
    const req = controller.expectOne('/api/users');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc123');
    req.flush({});
  });

  it('does not add token to login, register or forgot endpoints', () => {
    localStorage.setItem('mv_token', 'abc123');
    ['/auth/login', '/auth/register', '/auth/forgot'].forEach(url => {
      http.get(url).subscribe();
      const req = controller.expectOne(url);
      expect(req.request.headers.has('Authorization')).toBeFalse();
      req.flush({});
    });
  });

  it('does not add token to non-API URLs', () => {
    localStorage.setItem('mv_token', 'abc123');
    http.get('/public/config').subscribe();
    const req = controller.expectOne('/public/config');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
