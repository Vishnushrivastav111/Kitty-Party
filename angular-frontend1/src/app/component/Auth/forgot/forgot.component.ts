import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { DialogService } from '../../../shared/dialog.service';
import { Validators } from '../../../shared/format';

@Component({
  selector: 'app-forgot',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot.component.html',
  styleUrl: './forgot.component.scss',
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
