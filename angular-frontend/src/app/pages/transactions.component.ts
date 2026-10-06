import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { TX_CATEGORIES, Validators, formatDate, formatINR, queryRows, todayISO } from '../core/format';
import { Tx } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Transactions</h1><p>Track income and expenses with search, filters, and pagination</p></div>
    <div class="panel">
      <div class="panel-header">
        <h2>All transactions</h2>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Add transaction</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search name, category..." [ngModel]="search()" (ngModelChange)="setSearch($event)" />
        <select [ngModel]="type()" (ngModelChange)="setFilter('type', $event)">
          <option value="">All types</option><option value="income">Income</option><option value="expense">Expense</option>
        </select>
        <select [ngModel]="category()" (ngModelChange)="setFilter('category', $event)">
          <option value="">All categories</option>
          @for (item of categories; track item) { <option [value]="item">{{ item }}</option> }
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Date</th><th>Name</th><th>Category</th><th>Type</th><th>Amount</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="6" class="empty-state">No transactions found</td></tr> }
            @for (row of page().rows; track row.id; let i = $index) {
              <tr [class]="'row-tone-' + (i % 4)">
                <td>{{ date(row.date) }}</td><td>{{ row.name }}</td><td>{{ row.category }}</td>
                <td><span class="badge" [class.badge-success]="row.type === 'income'" [class.badge-warning]="row.type !== 'income'">{{ row.type }}</span></td>
                <td [class.amt-pos]="row.type === 'income'" [class.amt-neg]="row.type !== 'income'">{{ money(row.amount) }}</td>
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
        <h3>{{ form.id ? 'Edit transaction' : 'Add transaction' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Date</label><input type="date" name="date" [(ngModel)]="form.date" [max]="today" [class.input-error]="errors()['date']" /><span class="field-error" [style.display]="errors()['date'] ? 'block' : 'none'">{{ errors()['date'] }}</span></div>
            <div class="form-group"><label>Name</label><input name="name" [(ngModel)]="form.name" [class.input-error]="errors()['name']" /><span class="field-error" [style.display]="errors()['name'] ? 'block' : 'none'">{{ errors()['name'] }}</span></div>
            <div class="form-group"><label>Category</label>
              <select name="category" [(ngModel)]="form.category" [class.input-error]="errors()['category']"><option value="">Select</option>@for (item of categories; track item) { <option [value]="item">{{ item }}</option> }</select>
              <span class="field-error" [style.display]="errors()['category'] ? 'block' : 'none'">{{ errors()['category'] }}</span>
            </div>
            <div class="form-group"><label>Type</label><select name="type" [(ngModel)]="form.type"><option value="expense">Expense</option><option value="income">Income</option></select></div>
            <div class="form-group"><label>Amount (₹)</label><input type="number" name="amount" min="0" [(ngModel)]="form.amount" [class.input-error]="errors()['amount']" /><span class="field-error" [style.display]="errors()['amount'] ? 'block' : 'none'">{{ errors()['amount'] }}</span></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-outline" (click)="editing.set(false)">Cancel</button>
            <button type="submit" class="btn btn-primary" [disabled]="busy()">Save</button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class TransactionsComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  readonly categories = TX_CATEGORIES;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  readonly search = signal('');
  readonly type = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form = blankTx();
  readonly page = computed(() => queryRows(this.vault.transactions(), this.search(), ['name', 'category', 'type', 'date'], { type: this.type(), category: this.category() }, this.pageNo(), 6));

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setFilter(key: 'type' | 'category', value: string): void {
    if (key === 'type') this.type.set(value); else this.category.set(value);
    this.pageNo.set(1);
  }
  open(row: Tx | null): void {
    this.form = row ? { ...row, amount: Number(row.amount) } : blankTx();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = {
      date: Validators.dateNotFuture(this.form.date),
      name: Validators.required(this.form.name, 'Name'),
      category: Validators.required(this.form.category, 'Category'),
      amount: Validators.amount(this.form.amount),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean) || Number(this.form.amount) <= 0) {
      if (Number(this.form.amount) <= 0) this.errors.set({ ...errors, amount: 'Amount must be greater than zero' });
      return;
    }
    const body = { date: this.form.date, name: this.form.name.trim(), category: this.form.category, type: this.form.type, amount: Number(this.form.amount) };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateTx(this.form.id, body);
      else await this.vault.addTx(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save transaction');
    } finally { this.busy.set(false); }
  }
  async remove(row: Tx): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this transaction?')) await this.vault.deleteTx(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every transaction? This action cannot be undone.')) await this.vault.clearTx();
  }
}

function blankTx(): Tx {
  return { id: '', name: '', category: '', type: 'expense', amount: 0, date: todayISO() };
}
