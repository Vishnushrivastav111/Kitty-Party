import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DialogService } from '../core/dialog.service';
import { Validators } from '../core/format';

@Component({
  selector: 'app-forgot',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <section class="auth-visual">
        <div class="brand"><i class="fas fa-vault"></i> MicroVault</div>
        <h1>Reset your password</h1>
        <p>Verify your registered email and set a new secure password for your MicroVault account.</p>
      </section>
      <section class="auth-panel">
        <div class="auth-card">
          <h2>Forgot password</h2>
          <p class="subtitle">Each step is validated before you continue</p>
          <form (ngSubmit)="submit()" novalidate>
            <div class="auth-field">
              <label for="email">Registered email</label>
              <input id="email" type="email" name="email" [(ngModel)]="email" [class.input-error]="errors()['email']" />
              <span class="field-error" [style.display]="errors()['email'] ? 'block' : 'none'">{{ errors()['email'] }}</span>
            </div>
            <div class="auth-field">
              <label for="otp">Verification code</label>
              <input id="otp" name="otp" [(ngModel)]="otp" maxlength="6" placeholder="Enter 6-digit code" [class.input-error]="errors()['otp']" />
              <span class="field-error" [style.display]="errors()['otp'] ? 'block' : 'none'">{{ errors()['otp'] }}</span>
            </div>
            <button type="button" class="btn btn-outline" style="width:100%;margin-bottom:12px" (click)="sendCode()" [disabled]="sending()">
              {{ sending() ? 'Sending…' : 'Send verification code' }}
            </button>
            <div class="auth-field">
              <label for="password">New password</label>
              <input id="password" [type]="show() ? 'text' : 'password'" name="password" [(ngModel)]="password" style="padding-right:42px" [class.input-error]="errors()['password']" />
              <button type="button" class="toggle-pass" (click)="show.set(!show())"><i class="fas" [class.fa-eye]="!show()" [class.fa-eye-slash]="show()"></i></button>
              <span class="field-error" [style.display]="errors()['password'] ? 'block' : 'none'">{{ errors()['password'] }}</span>
            </div>
            <div class="auth-field">
              <label for="confirmPassword">Confirm new password</label>
              <input id="confirmPassword" [type]="show2() ? 'text' : 'password'" name="confirmPassword" [(ngModel)]="confirmPassword" style="padding-right:42px" [class.input-error]="errors()['confirmPassword']" />
              <button type="button" class="toggle-pass" (click)="show2.set(!show2())"><i class="fas" [class.fa-eye]="!show2()" [class.fa-eye-slash]="show2()"></i></button>
              <span class="field-error" [style.display]="errors()['confirmPassword'] ? 'block' : 'none'">{{ errors()['confirmPassword'] }}</span>
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%" [disabled]="busy()">Update password</button>
          </form>
          <p class="bottom-text"><a routerLink="/login">Back to login</a></p>
        </div>
      </section>
    </div>
  `,
})
export class ForgotComponent {
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(DialogService);
  private readonly router = inject(Router);

  email = '';
  otp = '';
  password = '';
  confirmPassword = '';
  readonly show = signal(false);
  readonly show2 = signal(false);
  readonly sending = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});

  async sendCode(): Promise<void> {
    const email = Validators.email(this.email.trim());
    this.errors.set({ email });
    if (email) return;
    this.sending.set(true);
    try {
      const res = await this.auth.sendResetOtp(this.email.trim());
      const message = res.demoOtp
        ? `Verification code (demo): ${res.demoOtp}`
        : 'Verification code sent to your email.';
      await this.dialog.notice(message, 'Verification code');
    } catch (error) {
      this.errors.set({ email: error instanceof Error ? error.message : 'No account found with this email' });
    } finally {
      this.sending.set(false);
    }
  }

  async submit(): Promise<void> {
    const errors: Record<string, string> = {
      email: Validators.email(this.email.trim()),
      otp: /^\d{6}$/.test(this.otp.trim()) ? '' : 'Enter the 6-digit verification code',
      password: Validators.password(this.password),
      confirmPassword: this.confirmPassword !== this.password ? 'Passwords do not match' : '',
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      await this.auth.resetPassword({
        email: this.email.trim(),
        otp: this.otp.trim(),
        password: this.password,
        confirmPassword: this.confirmPassword,
      });
      await this.dialog.notice('Password updated. You can sign in now.', 'Password reset');
      await this.router.navigateByUrl('/login');
    } catch (error) {
      this.errors.set({ ...errors, otp: error instanceof Error ? error.message : 'Could not reset password' });
    } finally {
      this.busy.set(false);
    }
  }
}
