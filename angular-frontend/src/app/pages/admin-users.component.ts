import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { Validators, formatDate, queryRows } from '../core/format';
import { VaultUser } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Manage users</h1><p>Admin CRUD for normal member accounts</p></div>
    <div class="panel">
      <div class="panel-header">
        <h2>Normal users</h2>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-user-plus"></i> Add user</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search name, email, phone..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="status()" (ngModelChange)="status.set($event); pageNo.set(1)">
          <option value="">All statuses</option><option value="active">Active</option><option value="inactive">Inactive</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Created</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="6" class="empty-state">No normal users found</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ row.fullName }}</td><td>{{ row.email }}</td><td>{{ row.phone || '—' }}</td><td>{{ date(row.createdAt) }}</td>
                <td><span class="badge" [class.badge-success]="row.status === 'active'" [class.badge-neutral]="row.status !== 'active'">{{ row.status || 'active' }}</span></td>
                <td>
                  <button class="btn btn-outline btn-sm" type="button" (click)="open(row)"><i class="fas fa-pen"></i></button>
                  <button class="btn btn-danger btn-sm" type="button" (click)="remove(row)"><i class="fas fa-trash"></i></button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
    <div class="modal-backdrop" [class.open]="editing()">
      <div class="modal">
        <h3>{{ form.id ? 'Edit user' : 'Add user' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Full name</label><input name="fullName" [(ngModel)]="form.fullName" /><span class="field-error" [style.display]="errors()['fullName'] ? 'block' : 'none'">{{ errors()['fullName'] }}</span></div>
            <div class="form-group"><label>Email</label><input type="email" name="email" [(ngModel)]="form.email" /><span class="field-error" [style.display]="errors()['email'] ? 'block' : 'none'">{{ errors()['email'] }}</span></div>
            <div class="form-group"><label>Phone</label><input name="phone" maxlength="10" [(ngModel)]="form.phone" /><span class="field-error" [style.display]="errors()['phone'] ? 'block' : 'none'">{{ errors()['phone'] }}</span></div>
            <div class="form-group"><label>Password</label><input type="password" name="password" [(ngModel)]="password" [placeholder]="form.id ? 'Leave blank to keep current' : 'Create password'" /><span class="field-error" [style.display]="errors()['password'] ? 'block' : 'none'">{{ errors()['password'] }}</span></div>
            <div class="form-group"><label>Status</label><select name="status" [(ngModel)]="form.status"><option value="active">Active</option><option value="inactive">Inactive</option></select></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-outline" (click)="editing.set(false)">Cancel</button>
            <button class="btn btn-primary" [disabled]="busy()">Save</button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class AdminUsersComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: VaultUser = blank();
  password = '';
  readonly page = computed(() => queryRows(this.vault.members(), this.search(), ['fullName', 'email', 'phone'], { status: this.status() }, this.pageNo(), 6));

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
    const duplicate = this.vault.members().find((row) => row.email.toLowerCase() === this.form.email.trim().toLowerCase() && row.id !== this.form.id);
    if (duplicate) errors['email'] = 'Email already in use';
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body: Record<string, unknown> = { fullName: this.form.fullName.trim(), email: this.form.email.trim().toLowerCase(), phone: (this.form.phone || '').trim(), status: this.form.status || 'active' };
    if (this.password) body['password'] = this.password;
    this.busy.set(true);
    try {
      await this.vault.saveMember(this.form.id || null, body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save user', 'Cannot save');
    } finally { this.busy.set(false); }
  }
  async remove(row: VaultUser): Promise<void> {
    if (!(await this.dialog.confirm('Are you sure you want to delete this user?'))) return;
    try { await this.vault.deleteMember(row.id); }
    catch (error) { await this.dialog.notice(error instanceof Error ? error.message : 'Could not delete user', 'Cannot delete'); }
  }
}

function blank(): VaultUser {
  return { id: '', fullName: '', email: '', phone: '', role: 'user', status: 'active' };
}
