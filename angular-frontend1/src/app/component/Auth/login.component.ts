import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { homePath, Validators } from '../../shared/format';
import { FinanceService } from '../../service/finance.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
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
