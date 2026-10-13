import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { assignError, assignErrors, confirmError, mergeApiFields } from '../../../shared/field-state';
import { joinFullName, personName } from '../../../shared/finance-rules';
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

  firstName = '';
  middleName = '';
  lastName = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';
  terms = localStorage.getItem('mv_termsAccepted') === 'true';
  readonly show = signal(false);
  readonly show2 = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');

  onFirst(value: string): void {
    this.firstName = value;
    this.errors.update((current) => assignError(current, 'firstName', personName(value, 'First name', true)));
  }

  onMiddle(value: string): void {
    this.middleName = value;
    this.errors.update((current) => assignError(current, 'middleName', personName(value, 'Middle name', false)));
  }

  onLast(value: string): void {
    this.lastName = value;
    this.errors.update((current) => assignError(current, 'lastName', personName(value, 'Last name', true)));
  }

  onEmail(value: string): void {
    this.email = value;
    this.errors.update((current) => assignError(current, 'email', Validators.email(value.trim())));
  }

  onPhone(value: string): void {
    this.phone = value;
    this.errors.update((current) => assignError(current, 'phone', Validators.phone(value.trim())));
  }

  onPassword(value: string): void {
    this.password = value;
    this.errors.update((current) => assignErrors(current, {
      password: Validators.password(value),
      confirmPassword: this.confirmPassword || current['confirmPassword'] ? confirmError(value, this.confirmPassword) : '',
    }));
  }

  onConfirm(value: string): void {
    this.confirmPassword = value;
    this.errors.update((current) => assignError(current, 'confirmPassword', confirmError(this.password, value)));
  }

  async submit(): Promise<void> {
    const fullName = joinFullName(this.firstName, this.middleName, this.lastName);
    const errors: Record<string, string> = {
      firstName: personName(this.firstName, 'First name', true),
      middleName: personName(this.middleName, 'Middle name', false),
      lastName: personName(this.lastName, 'Last name', true),
      email: Validators.email(this.email.trim()),
      phone: Validators.phone(this.phone.trim()),
      password: Validators.password(this.password),
      confirmPassword: this.confirmPassword !== this.password ? 'Passwords do not match' : '',
      terms: this.terms ? '' : 'Please accept the Terms & Conditions',
    };
    if (!errors['firstName'] && !errors['lastName'] && fullName.trim().length < 3) {
      errors['lastName'] = 'Enter a first and last name';
    }
    this.errors.set(errors);
    this.formError.set('');
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    const result = await this.auth.register({
      fullName,
      email: this.email.trim(),
      phone: this.phone.trim(),
      password: this.password,
      confirmPassword: this.confirmPassword,
    });
    this.busy.set(false);
    if (!result.ok) {
      this.formError.set(result.message || 'The server did not complete registration.');
      this.errors.set(mergeApiFields(errors, result.fieldErrors));
      return;
    }
    await this.router.navigateByUrl('/login');
  }
}
