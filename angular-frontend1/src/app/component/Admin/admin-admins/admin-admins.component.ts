import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { Validators, formatDate, queryRows } from '../../../shared/format';
import { VaultUser } from '../../../model/models';
import { AuthService } from '../../../service/auth.service';
import { AdminService } from '../../../service/admin.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-admin-admins',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './admin-admins.component.html',
  styleUrl: './admin-admins.component.scss',
})
export class AdminAdminsComponent {
  private readonly vault = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(DialogService);
  readonly date = formatDate;
  readonly me = computed(() => this.auth.user()?.id || '');
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: VaultUser = blank();
  password = '';
  readonly page = computed(() => queryRows(this.vault.admins(), this.search(), ['fullName', 'email', 'phone'], { status: this.status() }, this.pageNo(), 6));

  open(row: VaultUser | null): void {
    this.form = row ? { ...row, status: row.status || 'active' } : blank();
    this.password = '';
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors: Record<string, string> = {
      fullName: Validators.name(this.form.fullName),
      email: Validators.email(this.form.email.trim()),
      phone: Validators.phone((this.form.phone || '').trim()),
      password: this.form.id ? (this.password ? Validators.password(this.password) : '') : Validators.password(this.password),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body: Record<string, unknown> = { fullName: this.form.fullName.trim(), email: this.form.email.trim().toLowerCase(), phone: (this.form.phone || '').trim(), status: this.form.status || 'active', role: 'admin' };
    if (this.password) body['password'] = this.password;
    this.busy.set(true);
    try {
      await this.vault.saveAdmin(this.form.id || null, body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save admin');
    } finally { this.busy.set(false); }
  }
  async remove(row: VaultUser): Promise<void> {
    if (row.id === this.me()) return;
    if (await this.dialog.confirm('Are you sure you want to delete this admin?')) {
      try { await this.vault.deleteAdmin(row.id); }
      catch (error) { await this.dialog.notice(error instanceof Error ? error.message : 'Could not delete admin', 'Cannot delete'); }
    }
  }
}

function blank(): VaultUser {
  return { id: '', fullName: '', email: '', phone: '', role: 'admin', status: 'active' };
}
