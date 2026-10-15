import { Component, computed, inject, signal } from '@angular/core';
import { LOGIN_NOTICE_KEY } from '../../service/session.interceptor';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { assignError, assignErrors } from '../../shared/field-state';
import { homePath, Validators } from '../../shared/format';
import { SiteFooterComponent } from '../../shared/site-footer.component';
import { FinanceService } from '../../service/finance.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, SiteFooterComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly vault = inject(FinanceService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  readonly show = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');
  /** Shown when the member was signed out, for example because the account was blocked. */
  readonly notice = signal(sessionStorage.getItem(LOGIN_NOTICE_KEY) || '');
  readonly blocked = computed(() => /blocked/i.test(this.formError()));

  constructor() {
    sessionStorage.removeItem(LOGIN_NOTICE_KEY);
  }

  onEmail(value: string): void {
    this.email = value;
    this.formError.set('');
    this.errors.update((current) => assignError(current, 'email', Validators.email(value.trim())));
  }

  onPassword(value: string): void {
    this.password = value;
    this.formError.set('');
    const message = !value ? 'Password is required' : value.length < 8 ? 'Password must be at least 8 characters' : '';
    this.errors.update((current) => assignError(current, 'password', message));
  }

  async submit(): Promise<void> {
    const emailErr = Validators.email(this.email.trim());
    const passwordErr = !this.password ? 'Password is required' : this.password.length < 8 ? 'Password must be at least 8 characters' : '';
    this.errors.set({ email: emailErr, password: passwordErr });
    this.formError.set('');
    if (emailErr || passwordErr) return;
    this.busy.set(true);
    const result = await this.auth.login(this.email.trim(), this.password);
    if (!result.ok || !result.user) {
      this.formError.set(result.message || 'We could not sign you in. Please try again.');
      this.errors.update((current) => assignErrors(current, result.fieldErrors || {}));
      this.busy.set(false);
      return;
    }
    try {
      await this.vault.load();
    } catch {
      /* route still opens; shell will surface the load error */
    }
    const role = result.user.role;
    if (role === 'admin' || role === 'superadmin') {
      await this.router.navigateByUrl(homePath(role));
    } else if (this.vault.finance() || this.vault.setupSkipped()) {
      await this.router.navigateByUrl('/dashboard');
    } else {
      await this.router.navigateByUrl('/setup');
    }
    this.busy.set(false);
  }
}
