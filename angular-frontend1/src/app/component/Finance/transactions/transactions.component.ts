import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { parseApiError } from '../../../shared/api-error';
import { DialogService } from '../../../shared/dialog.service';
import { assignError } from '../../../shared/field-state';
import { monthExpenseTotal, monthExpenseTotalWithDraft, overrunIncrease } from '../../../shared/finance-rules';
import { TX_CATEGORIES, Validators, formatDate, formatINR, queryRows, todayISO } from '../../../shared/format';
import { Tx } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({ selector: 'app-transactions', standalone: true, imports: [FormsModule, PagerComponent], templateUrl: './transactions.component.html', styleUrl: './transactions.component.scss' })
export class TransactionsComponent {
  private readonly vault = inject(FinanceService); private readonly dialog = inject(DialogService);
  readonly categories = TX_CATEGORIES; readonly today = todayISO(); readonly money = formatINR; readonly date = formatDate;
  readonly search = signal(''); readonly type = signal(''); readonly category = signal(''); readonly pageNo = signal(1); readonly editing = signal(false); readonly busy = signal(false); readonly errors = signal<Record<string, string>>({});
  form = blankTx();
  readonly page = computed(() => queryRows(this.vault.transactions(), this.search(), ['name', 'category', 'type', 'date'], { type: this.type(), category: this.category() }, this.pageNo(), 6));
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setFilter(key: 'type' | 'category', value: string): void { if (key === 'type') this.type.set(value); else this.category.set(value); this.pageNo.set(1); }
  open(row: Tx | null): void { this.form = row ? { ...row, amount: Number(row.amount) } : blankTx(); this.errors.set({}); this.editing.set(true); }
  async save(): Promise<void> {
    const errors = { date: Validators.dateNotFuture(this.form.date), name: Validators.required(this.form.name, 'Name'), category: Validators.required(this.form.category, 'Category'), amount: Validators.amount(this.form.amount) };
    this.errors.set(errors); if (Object.values(errors).some(Boolean) || Number(this.form.amount) <= 0) { if (Number(this.form.amount) <= 0) this.errors.set({ ...errors, amount: 'Amount must be greater than zero' }); return; }
    const body = { date: this.form.date, name: this.form.name.trim(), category: this.form.category, type: this.form.type, amount: Number(this.form.amount) }; this.busy.set(true);
    try { const warning = this.form.id ? await this.vault.updateTx(this.form.id, body) : await this.vault.addTx(body); this.editing.set(false); if (warning) await this.dialog.notice(warning, 'Saved, refresh failed'); }
    catch (error) { const parsed = parseApiError(error); this.errors.set({ ...errors, ...parsed.fieldErrors, form: parsed.message }); } finally { this.busy.set(false); }
  }
  async remove(row: Tx): Promise<void> {
    const confirmed = await this.dialog.confirm('Submit this transaction for administrator approval? The transaction will remain visible until an administrator approves the deletion.', { title: 'Request deletion', okText: 'Submit request', cancelText: 'Cancel' });
    if (!confirmed) return;
    this.busy.set(true);
    try { const request = await this.vault.requestTransactionDeletion(row.id); await this.dialog.notice(`Deletion request submitted successfully. Status: ${request.status}.`, 'Approval request submitted'); }
    catch (error) { await this.dialog.notice(parseApiError(error).message, 'Request failed'); } finally { this.busy.set(false); }
  }
  overrunPreview(): number { if (this.form.type !== 'expense') return 0; const month = this.today.slice(0, 7); const draft = { id: this.form.id, type: this.form.type, amount: Number(this.form.amount), date: this.form.date }; const before = monthExpenseTotal(this.vault.transactions(), month, this.form.id || undefined); const after = monthExpenseTotalWithDraft(this.vault.transactions(), month, draft); return overrunIncrease(this.vault.position().monthlyBudget, before, after); }
  dateOk(value: string): string { return Validators.dateNotFuture(value); } nameOk(value: string): string { return Validators.required(value, 'Name'); } categoryOk(value: string): string { return Validators.required(value, 'Category'); } amountOk(value: unknown): string { return Number(value) > 0 ? '' : 'Amount must be greater than zero'; }
}
function blankTx(): Tx { return { id: '', name: '', category: '', type: 'expense', amount: 0, date: todayISO() }; }
