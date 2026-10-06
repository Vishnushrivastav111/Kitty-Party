import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DialogService } from '../core/dialog.service';
import { ROLES, Validators } from '../core/format';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-header"><h1>Settings</h1><p>Profile, security, and financial setup</p></div>
    <div class="panel">
      <h2 style="margin-bottom:14px;color:var(--navy)">Profile</h2>
      <form (ngSubmit)="saveProfile()" novalidate>
        <div class="form-grid">
          <div class="form-group"><label>Full name</label><input name="fullName" [(ngModel)]="fullName" /><span class="field-error" [style.display]="errors()['fullName'] ? 'block' : 'none'">{{ errors()['fullName'] }}</span></div>
          <div class="form-group"><label>Email</label><input type="email" name="email" [(ngModel)]="email" /><span class="field-error" [style.display]="errors()['email'] ? 'block' : 'none'">{{ errors()['email'] }}</span></div>
          <div class="form-group"><label>Phone</label><input name="phone" maxlength="10" [(ngModel)]="phone" /><span class="field-error" [style.display]="errors()['phone'] ? 'block' : 'none'">{{ errors()['phone'] }}</span></div>
        </div>
        <div class="form-actions"><button class="btn btn-primary" [disabled]="busy()">Save profile</button></div>
      </form>
    </div>
    <div class="panel">
      <h2 style="margin-bottom:14px;color:var(--navy)">Change password</h2>
      <form (ngSubmit)="savePassword()" novalidate>
        <div class="form-grid">
          <div class="form-group"><label>Current password</label><input type="password" name="current" [(ngModel)]="currentPassword" /><span class="field-error" [style.display]="passErrors()['current'] ? 'block' : 'none'">{{ passErrors()['current'] }}</span></div>
          <div class="form-group"><label>New password</label><input type="password" name="next" [(ngModel)]="newPassword" /><span class="field-error" [style.display]="passErrors()['next'] ? 'block' : 'none'">{{ passErrors()['next'] }}</span></div>
          <div class="form-group"><label>Confirm password</label><input type="password" name="confirm" [(ngModel)]="confirmPassword" /><span class="field-error" [style.display]="passErrors()['confirm'] ? 'block' : 'none'">{{ passErrors()['confirm'] }}</span></div>
        </div>
        <div class="form-actions"><button class="btn btn-navy" [disabled]="passBusy()">Update password</button></div>
      </form>
    </div>
    @if (member()) {
      <div class="panel">
        <h2 style="margin-bottom:10px;color:var(--navy)">Financial setup</h2>
        <p style="color:var(--muted);margin-bottom:12px;font-size:14px">Revisit income, expenses, and budget baselines.</p>
        <a class="btn btn-outline" routerLink="/setup"><i class="fas fa-sliders"></i> Open financial setup</a>
      </div>
    }
  `,
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
