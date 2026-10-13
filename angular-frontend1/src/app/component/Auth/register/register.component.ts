import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { Validators } from '../../../shared/format';

@Component({ selector: 'app-register', standalone: true, imports: [FormsModule, RouterLink], templateUrl: './register.component.html', styleUrl: './register.component.scss' })
export class RegisterComponent {
  private readonly auth = inject(AuthService); private readonly router = inject(Router);
  firstName = ''; middleName = ''; lastName = ''; email = ''; phone = ''; password = ''; confirmPassword = '';
  terms = localStorage.getItem('mv_termsAccepted') === 'true';
  readonly show = signal(false); readonly show2 = signal(false); readonly busy = signal(false); readonly errors = signal<Record<string, string>>({});

  private fullName(): string { return [this.firstName, this.middleName, this.lastName].map(v => v.trim()).filter(Boolean).join(' '); }
  validate(field: string): void {
    const next = { ...this.errors() };
    const value = field === 'firstName' ? this.firstName : field === 'middleName' ? this.middleName : field === 'lastName' ? this.lastName : field === 'email' ? this.email : field === 'phone' ? this.phone : field === 'password' ? this.password : this.confirmPassword;
    if (field === 'firstName') next.firstName = Validators.name(value);
    if (field === 'lastName') next.lastName = Validators.name(value);
    if (field === 'email') next.email = Validators.email(this.email.trim());
    if (field === 'phone') next.phone = Validators.phone(this.phone.trim());
    if (field === 'password') next.password = Validators.password(this.password);
    if (field === 'confirmPassword') next.confirmPassword = this.confirmPassword !== this.password ? 'Passwords do not match' : '';
    if (field === 'middleName') next.middleName = '';
    this.errors.set(next);
  }

  async submit(): Promise<void> {
    const errors: Record<string, string> = { firstName: Validators.name(this.firstName), lastName: Validators.name(this.lastName), email: Validators.email(this.email.trim()), phone: Validators.phone(this.phone.trim()), password: Validators.password(this.password), confirmPassword: this.confirmPassword !== this.password ? 'Passwords do not match' : '', terms: this.terms ? '' : 'Please accept the Terms & Conditions' };
    this.errors.set(errors); if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    const result = await this.auth.register({ fullName: this.fullName(), email: this.email.trim(), phone: this.phone.trim(), password: this.password, confirmPassword: this.confirmPassword });
    this.busy.set(false);
    if (!result.ok) { this.errors.set({ ...errors, email: result.message || 'Registration failed' }); return; }
    await this.router.navigateByUrl('/login');
  }
}
