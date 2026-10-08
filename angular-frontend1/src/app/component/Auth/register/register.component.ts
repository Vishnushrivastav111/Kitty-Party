import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { Validators } from '../../../shared/format';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  fullName = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';
  terms = localStorage.getItem('mv_termsAccepted') === 'true';
  readonly show = signal(false);
  readonly show2 = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});

  async submit(): Promise<void> {
    const errors: Record<string, string> = {
      fullName: Validators.name(this.fullName),
      email: Validators.email(this.email.trim()),
      phone: Validators.phone(this.phone.trim()),
      password: Validators.password(this.password),
      confirmPassword: this.confirmPassword !== this.password ? 'Passwords do not match' : '',
      terms: this.terms ? '' : 'Please accept the Terms & Conditions',
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    const result = await this.auth.register({
      fullName: this.fullName.trim(),
      email: this.email.trim(),
      phone: this.phone.trim(),
      password: this.password,
      confirmPassword: this.confirmPassword,
    });
    this.busy.set(false);
    if (!result.ok) {
      this.errors.set({ ...errors, email: result.message || 'Email already registered' });
      return;
    }
    await this.router.navigateByUrl('/login');
  }
}
