import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { homePath, Validators } from '../core/format';
import { VaultService } from '../core/vault.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <section class="auth-visual">
        <div class="brand"><i class="fas fa-vault"></i> MicroVault</div>
        <h1>Business-ready personal finance control</h1>
        <p>Track savings, budgets, goals, and spending with role-based access for members, admins, and super administrators.</p>
      </section>
      <section class="auth-panel">
        <div class="auth-card">
          <h2>Sign in</h2>
          <p class="subtitle">Access your MicroVault workspace securely</p>
          <form (ngSubmit)="submit()" novalidate>
            <div class="auth-field">
              <label for="email">Email address</label>
              <input id="email" type="email" name="email" [(ngModel)]="email" placeholder="name@company.com" autocomplete="username"
                [class.input-error]="errors()['email']" [class.input-success]="!errors()['email'] && email" />
              <span class="field-error" [style.display]="errors()['email'] ? 'block' : 'none'">{{ errors()['email'] }}</span>
            </div>
            <div class="auth-field">
              <label for="password">Password</label>
              <input id="password" [type]="show() ? 'text' : 'password'" name="password" [(ngModel)]="password" placeholder="Enter password"
                autocomplete="current-password" style="padding-right:42px"
                [class.input-error]="errors()['password']" [class.input-success]="!errors()['password'] && password" />
              <button type="button" class="toggle-pass" aria-label="Show password" (click)="show.set(!show())">
                <i class="fas" [class.fa-eye]="!show()" [class.fa-eye-slash]="show()"></i>
              </button>
              <span class="field-error" [style.display]="errors()['password'] ? 'block' : 'none'">{{ errors()['password'] }}</span>
            </div>
            <div class="auth-links">
              <a routerLink="/forgot-password">Forgot password?</a>
              <a routerLink="/">Back to home</a>
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%" [disabled]="busy()">{{ busy() ? 'Signing in…' : 'Login' }}</button>
          </form>
          <p class="bottom-text">Don't have an account? <a routerLink="/register">Register</a></p>
        </div>
      </section>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly vault = inject(VaultService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  readonly show = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});

  async submit(): Promise<void> {
    const emailErr = Validators.email(this.email.trim());
    const passwordErr = !this.password ? 'Password is required' : this.password.length < 8 ? 'Password must be at least 8 characters' : '';
    this.errors.set({ email: emailErr, password: passwordErr });
    if (emailErr || passwordErr) return;
    this.busy.set(true);
    const result = await this.auth.login(this.email.trim(), this.password);
    if (!result.ok || !result.user) {
      this.errors.set({ email: '', password: result.message || 'Invalid email or password' });
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
