import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { assignError, assignErrors, confirmError, mergeApiFields } from '../../../shared/field-state';
import { joinFullName, personName } from '../../../shared/finance-rules';
import { Validators } from '../../../shared/format';
import { SiteFooterComponent } from '../../../shared/site-footer.component';
import { TermsDialogComponent } from '../terms/terms-dialog.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, SiteFooterComponent, TermsDialogComponent],
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
  terms = false;
  private termsRead = false;
  readonly termsOpen = signal(false);
  readonly show = signal(false);
  readonly show2 = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly created = signal(false);

  openTerms(event: Event): void {
    event.preventDefault();
    this.termsOpen.set(true);
  }

  /** The box can be ticked only after the terms were read in the dialog; otherwise warn and leave it unticked. */
  onTermsClick(event: Event): void {
    if (!this.termsRead) {
      event.preventDefault();
      this.errors.update((current) => ({ ...current, terms: 'Please read the Terms & Conditions first. Open them from the link, scroll to the end, then accept.' }));
      return;
    }
    this.terms = (event.target as HTMLInputElement).checked;
    this.errors.update((current) => assignError(current, 'terms', this.terms ? '' : 'Please accept the Terms & Conditions'));
  }

  acceptTerms(): void {
    this.termsRead = true;
    this.terms = true;
    this.termsOpen.set(false);
    this.errors.update((current) => assignError(current, 'terms', ''));
  }

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
      terms: this.terms && this.termsRead ? '' : this.termsRead ? 'Please accept the Terms & Conditions' : 'Please read the Terms & Conditions first. Open them from the link, scroll to the end, then accept.',
    };
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
      this.formError.set(result.message || 'We could not complete your registration. Please try again.');
      this.errors.set(mergeApiFields(errors, result.fieldErrors));
      this.created.set(false);
      return;
    }
    this.formError.set('');
    this.created.set(true);
  }
}
