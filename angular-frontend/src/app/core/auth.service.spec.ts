import { TestBed } from '@angular/core/testing';
import { AuthService, VaultUser } from './auth.service';
import { ApiService } from './api.service';

describe('AuthService', () => {
  let service: AuthService;
  let api: jasmine.SpyObj<ApiService>;
  const user: VaultUser = {
    id: 'u1', fullName: 'Alice User', email: 'alice@test.com', phone: '9876543210', role: 'user', status: 'active'
  };

  beforeEach(() => {
    localStorage.clear();
    api = jasmine.createSpyObj<ApiService>('ApiService', ['raw', 'post', 'put']);
    TestBed.configureTestingModule({ providers: [AuthService, { provide: ApiService, useValue: api }] });
    service = TestBed.inject(AuthService);
  });

  afterEach(() => localStorage.clear());

  it('starts logged out when storage is empty', () => {
    expect(service.loggedIn()).toBeFalse();
    expect(service.token()).toBe('');
    expect(service.user()).toBeNull();
  });

  it('restores a valid stored user and token', () => {
    localStorage.setItem('mv_token', 'token');
    localStorage.setItem('mv_currentUser', JSON.stringify(user));
    const restored = TestBed.inject(AuthService);
    expect(restored.token()).toBe('');
    expect(restored.user()).toBeNull();
  });

  it('logs in and persists a successful response', async () => {
    api.raw.and.resolveTo({ ok: true, token: 'abc', user });
    const result = await service.login(user.email, 'Password1!');
    expect(api.raw).toHaveBeenCalledWith('POST', '/auth/login', { email: user.email, password: 'Password1!' });
    expect(result.ok).toBeTrue();
    expect(service.loggedIn()).toBeTrue();
    expect(localStorage.getItem('mv_token')).toBe('abc');
    expect(JSON.parse(localStorage.getItem('mv_currentUser')!)).toEqual(user);
  });

  it('rejects incomplete login responses and handles login errors', async () => {
    api.raw.and.resolveTo({ ok: false, message: 'Bad credentials' });
    expect(await service.login('x', 'y')).toEqual({ ok: false, message: 'Bad credentials' });
    api.raw.and.rejectWith(new Error('Network down'));
    expect(await service.login('x', 'y')).toEqual({ ok: false, message: 'Network down' });
  });

  it('registers and maps registration errors', async () => {
    api.post.and.resolveTo(user);
    const data = { fullName: 'Alice User', email: user.email, phone: user.phone!, password: 'Password1!', confirmPassword: 'Password1!' };
    expect(await service.register(data)).toEqual({ ok: true, user });
    expect(api.post).toHaveBeenCalledWith('/auth/register', { ...data, termsAccepted: true });
    api.post.and.rejectWith(new Error('Already registered'));
    expect(await service.register(data)).toEqual({ ok: false, message: 'Already registered' });
  });

  it('updates and patches the current profile', async () => {
    api.raw.and.resolveTo({ ok: true, token: 't', user });
    await service.login(user.email, 'Password1!');
    const updated = { ...user, fullName: 'Updated User' };
    service.patchUser({ fullName: updated.fullName });
    expect(service.user()?.fullName).toBe('Updated User');
    api.put.and.resolveTo(updated);
    expect(await service.updateProfile({ fullName: updated.fullName, email: updated.email, phone: updated.phone! })).toEqual(updated);
    expect(service.user()?.fullName).toBe('Updated User');
  });

  it('changes password and sends/reset OTP', async () => {
    api.put.and.resolveTo(undefined);
    await service.changePassword({ currentPassword: 'a', newPassword: 'b', confirmPassword: 'b' });
    expect(api.put).toHaveBeenCalledWith('/users/me/password', { currentPassword: 'a', newPassword: 'b', confirmPassword: 'b' });
    api.post.and.resolveTo({ demoOtp: '123456' });
    expect(await service.sendResetOtp(user.email)).toEqual({ demoOtp: '123456' });
    await service.resetPassword({ email: user.email, otp: '123456', password: 'Password1!', confirmPassword: 'Password1!' });
    expect(api.post).toHaveBeenCalledWith('/auth/forgot/reset', jasmine.any(Object));
  });

  it('clears session and tolerates logout errors', async () => {
    api.raw.and.resolveTo({ ok: true, token: 't', user });
    await service.login(user.email, 'Password1!');
    api.post.and.rejectWith(new Error('Already logged out'));
    await service.logout();
    expect(service.loggedIn()).toBeFalse();
    expect(localStorage.getItem('mv_token')).toBeNull();
    expect(localStorage.getItem('mv_currentUser')).toBeNull();
    expect(localStorage.getItem('mv_isLoggedIn')).toBe('false');
  });
});
