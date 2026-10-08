import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { DialogService } from '../../shared/dialog.service';
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
  fullName = this.auth.user()?.fullName || '';
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

  async saveProfile(): Promise<void> {
    const errors = { fullName: Validators.name(this.fullName), email: Validators.email(this.email.trim()), phone: Validators.phone(this.phone.trim()) };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      await this.auth.updateProfile({ fullName: this.fullName.trim(), email: this.email.trim(), phone: this.phone.trim() });
      await this.dialog.notice('Profile saved', 'Settings');
    } catch (error) {
      this.errors.set({ ...errors, email: error instanceof Error ? error.message : 'Could not save profile' });
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
      this.passErrors.set({ ...errors, current: error instanceof Error ? error.message : 'Could not update password' });
    } finally { this.passBusy.set(false); }
  }
}
