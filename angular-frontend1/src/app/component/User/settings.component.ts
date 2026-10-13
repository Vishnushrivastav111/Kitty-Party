import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { DialogService } from '../../shared/dialog.service';
import { parseApiError } from '../../shared/api-error';
import { assignError, assignErrors, mergeApiFields } from '../../shared/field-state';
import { joinFullName, personName, splitFullName } from '../../shared/finance-rules';
import { ROLES, Validators } from '../../shared/format';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(DialogService);
  firstName = splitFullName(this.auth.user()?.fullName || '').firstName;
  middleName = splitFullName(this.auth.user()?.fullName || '').middleName;
  lastName = splitFullName(this.auth.user()?.fullName || '').lastName;
  email = this.auth.user()?.email || '';
  phone = this.auth.user()?.phone || '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  readonly member = signal(this.auth.user()?.role === ROLES.USER);
  readonly busy = signal(false);
  readonly passBusy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly passErrors = signal<Record<string, string>>({});

  onEmail(value: string): void {
    this.email = value;
    this.errors.update((current) => assignError(current, 'email', Validators.email(value.trim())));
  }
  onPhone(value: string): void {
    this.phone = value;
    this.errors.update((current) => assignError(current, 'phone', Validators.phone(value.trim())));
  }
  onName(field: 'firstName' | 'middleName' | 'lastName', value: string, label: string, required: boolean): void {
    this[field] = value;
    this.errors.update((current) => assignError(current, field, personName(value, label, required)));
  }
  async saveProfile(): Promise<void> {
    const fullName = joinFullName(this.firstName, this.middleName, this.lastName);
    const errors = {
      firstName: personName(this.firstName, 'First name', true),
      middleName: personName(this.middleName, 'Middle name', false),
      lastName: personName(this.lastName, 'Last name', true),
      email: Validators.email(this.email.trim()),
      phone: Validators.phone(this.phone.trim()),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      await this.auth.updateProfile({ fullName, email: this.email.trim(), phone: this.phone.trim() });
      await this.dialog.notice('Profile saved', 'Settings');
    } catch (error) {
      const parsed = parseApiError(error);
      this.errors.set({ ...mergeApiFields(errors, parsed.fieldErrors), form: parsed.message });
    } finally { this.busy.set(false); }
  }

  async savePassword(): Promise<void> {
    const errors = {
      current: this.currentPassword ? '' : 'Current password is required',
      next: Validators.password(this.newPassword),
      confirm: this.confirmPassword !== this.newPassword ? 'Passwords do not match' : '',
    };
    this.passErrors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.passBusy.set(true);
    try {
      await this.auth.changePassword({ currentPassword: this.currentPassword, newPassword: this.newPassword, confirmPassword: this.confirmPassword });
      this.currentPassword = this.newPassword = this.confirmPassword = '';
      await this.dialog.notice('Password updated', 'Settings');
    } catch (error) {
      const parsed = parseApiError(error);
      this.passErrors.set({ ...errors, ...parsed.fieldErrors, current: parsed.fieldErrors['current'] || parsed.message });
    } finally { this.passBusy.set(false); }
  }
  onConfirm(value: string): void {
    this.confirmPassword = value;
    this.passErrors.update((current) => assignError(current, 'confirm', value !== this.newPassword ? 'Passwords do not match' : ''));
  }
  onCurrent(value: string): void {
    this.currentPassword = value;
    this.passErrors.update((current) => assignError(current, 'current', value ? '' : 'Current password is required'));
  }
  onNewPassword(value: string): void {
    this.newPassword = value;
    this.passErrors.update((current) => assignErrors(current, {
      next: Validators.password(value),
      confirm: this.confirmPassword || current['confirm'] ? (this.confirmPassword !== value ? 'Passwords do not match' : '') : '',
    }));
  }
}
