import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { TX_CATEGORIES, Validators, formatDate, formatINR, queryRows, todayISO } from '../../../shared/format';
import { Tx } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss',
})
export class TransactionsComponent {
  private readonly vault = inject(FinanceService);
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
