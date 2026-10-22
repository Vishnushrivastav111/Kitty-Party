import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse, provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError, ApiService } from './api.service';
import { environment } from '../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends GET and unwraps a data envelope', async () => {
    const promise = service.get<{ id: number }>('/users/me');
    const req = http.expectOne(environment.apiBase + '/users/me');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Accept')).toBe('application/json');
    req.flush(JSON.stringify({ data: { id: 1 } }));
    await expectAsync(promise).toBeResolvedTo({ id: 1 });
  });

  it('returns raw payload when raw is used', async () => {
    const promise = service.raw<{ data: number }>('PATCH', '/x', { value: 2 });
    const req = http.expectOne(environment.apiBase + '/x');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ value: 2 });
    req.flush(JSON.stringify({ data: 10 }));
    await expectAsync(promise).toBeResolvedTo({ data: 10 });
  });

  it('supports POST, PUT and DELETE', async () => {
    const post = service.post('/a', { x: 1 });
    const postReq = http.expectOne(environment.apiBase + '/a');
    expect(postReq.request.method).toBe('POST');
    postReq.flush('{}');
    await expectAsync(post).toBeResolvedTo({});

    const put = service.put('/a/1', { x: 2 });
    const putReq = http.expectOne(environment.apiBase + '/a/1');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush('{"data":{"ok":true}}');
    await expectAsync(put).toBeResolvedTo({ ok: true });

    const del = service.delete('/a/1');
    const delReq = http.expectOne(environment.apiBase + '/a/1');
    expect(delReq.request.method).toBe('DELETE');
    delReq.flush('');
    await expectAsync(del).toBeResolvedTo(undefined);
  });

  it('handles empty and primitive JSON responses', async () => {
    const empty = service.get('/empty');
    http.expectOne(environment.apiBase + '/empty').flush('   ');
    await expectAsync(empty).toBeResolvedTo(undefined);

    const primitive = service.get<number>('/number');
    http.expectOne(environment.apiBase + '/number').flush('42');
    await expectAsync(primitive).toBeResolvedTo(42);
  });

  it('maps structured HTTP errors to ApiError', async () => {
    const promise = service.get('/failure');
    const req = http.expectOne(environment.apiBase + '/failure');
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    await expectAsync(promise).toBeRejectedWithError(ApiError, 'Forbidden');
    try {
      await promise;
    } catch (error) {
      expect(error).toEqual(jasmine.objectContaining({ status: 403 }));
    }
  });

  it('uses HTTP error message when structured message is absent', async () => {
    const promise = service.get('/failure');
    const req = http.expectOne(environment.apiBase + '/failure');
    req.flush('bad', { status: 500, statusText: 'Server Error' });
    await expectAsync(promise).toBeRejected();
    try {
      await promise;
    } catch (error) {
      expect(error instanceof ApiError).toBeTrue();
      expect((error as ApiError).status).toBe(500);
      expect((error as ApiError).message).toContain('Http failure response');
    }
  });
});
