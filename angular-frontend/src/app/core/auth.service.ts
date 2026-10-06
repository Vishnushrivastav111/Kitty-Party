import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';

export interface VaultUser {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  role: string;
  status?: string;
  createdAt?: string;
  setupComplete?: boolean;
}

const TOKEN_KEY = 'mv_token';
const USER_KEY = 'mv_currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  readonly user = signal<VaultUser | null>(this.readUser());
  readonly token = signal(localStorage.getItem(TOKEN_KEY) || '');
  readonly loggedIn = computed(() => !!this.token() && !!this.user());

  async login(email: string, password: string): Promise<{ ok: boolean; message?: string; user?: VaultUser }> {
    try {
      const res = await this.api.raw<{ ok: boolean; token?: string; message?: string; user?: VaultUser }>(
        'POST',
        '/auth/login',
        { email, password },
      );
      if (!res?.ok || !res.token || !res.user) {
        return { ok: false, message: res?.message || 'Invalid email or password' };
      }
      this.persist(res.token, res.user);
      return { ok: true, user: res.user };
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : 'Invalid email or password' };
    }
  }

  async register(data: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
    confirmPassword: string;
  }): Promise<{ ok: boolean; message?: string; user?: VaultUser }> {
    try {
      const user = await this.api.post<VaultUser>('/auth/register', { ...data, termsAccepted: true });
      return { ok: true, user };
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : 'Email already registered' };
    }
  }

  async logout(): Promise<void> {
    try {
      await this.api.post('/auth/logout', {});
    } catch {
      /* session may already be gone */
    }
    this.clearSession();
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.setItem('mv_isLoggedIn', 'false');
    this.token.set('');
    this.user.set(null);
  }

  patchUser(user: Partial<VaultUser> & { id?: string }): void {
    const current = this.user();
    if (!current) return;
    const next = { ...current, ...user, id: String(user.id || current.id) };
    this.user.set(next);
    localStorage.setItem(USER_KEY, JSON.stringify(next));
  }

  async updateProfile(data: { fullName: string; email: string; phone: string }): Promise<VaultUser> {
    const row = await this.api.put<VaultUser>('/users/me', data);
    this.patchUser(row);
    return row;
  }

  changePassword(data: { currentPassword: string; newPassword: string; confirmPassword: string }): Promise<void> {
    return this.api.put('/users/me/password', data);
  }

  sendResetOtp(email: string): Promise<{ demoOtp?: string }> {
    return this.api.post('/auth/forgot/send-otp', { email });
  }

  resetPassword(data: { email: string; otp: string; password: string; confirmPassword: string }): Promise<void> {
    return this.api.post('/auth/forgot/reset', data);
  }

  private persist(token: string, user: VaultUser): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem('mv_isLoggedIn', 'true');
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.token.set(token);
    this.user.set(user);
  }

  private readUser(): VaultUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as VaultUser) : null;
    } catch {
      return null;
    }
  }
}
