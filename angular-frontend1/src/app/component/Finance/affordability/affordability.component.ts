import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { parseApiError } from '../../../shared/api-error';
import { DialogService } from '../../../shared/dialog.service';
import { assignError } from '../../../shared/field-state';
import { Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { AffordCheck } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-affordability',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './affordability.component.html',
  styleUrl: './affordability.component.scss',
})
export class AffordabilityComponent {
  readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly router = inject(Router);
  paymentMethod = 'cash';
  emiMonths: number | null = null;
  annualInterestPercent: number | null = 0;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  itemName = '';
  amount: number | null = null;
  checkDate = todayISO();
  priority = 'Need';
  readonly result = signal<AffordCheck | null>(null);
  readonly search = signal('');
  readonly level = signal('');
  readonly pageNo = signal(1);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly recheckHint = signal('');
  readonly page = computed(() => queryRows(this.vault.affordChecks(), this.search(), ['itemName', 'verdict', 'priority'], { level: this.level() }, this.pageNo(), 5));

  async check(): Promise<void> {
    const errors = {
      itemName: Validators.required(this.itemName, 'Item name'),
      amount: Number(this.amount) > 0 ? '' : 'Amount must be greater than zero',
      checkDate: Validators.dateNotFuture(this.checkDate, 'Check date'),
    };
    this.errors.set(errors);
    this.formError.set('');
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      const row = await this.vault.checkAfford({
        itemName: this.itemName.trim(),
        amount: Number(this.amount),
        checkDate: this.checkDate,
        priority: this.priority,
        paymentMethod: this.paymentMethod,
        emiMonths: this.paymentMethod === 'emi' ? Number(this.emiMonths) : null,
        annualInterestPercent: this.paymentMethod === 'emi' ? Number(this.annualInterestPercent) || 0 : null,
      });
      this.result.set(row);
      this.recheckHint.set('');
    } catch (error) {
      const parsed = parseApiError(error);
      this.formError.set(parsed.message);
      this.errors.update((current) => ({ ...current, ...parsed.fieldErrors }));
    } finally { this.busy.set(false); }
  }
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  itemOk(value: string): string { return Validators.required(value, 'Item name'); }
  amountOk(value: unknown): string { return Number(value) > 0 ? '' : 'Amount must be greater than zero'; }
  dateOk(value: string): string { return Validators.dateNotFuture(value, 'Check date'); }
  async recheck(): Promise<void> {
    const last = this.result() || this.vault.affordChecks()[0];
    if (!last?.itemName || !(Number(last.amount) > 0)) {
      this.recheckHint.set('Run a new check. The previous purchase details are not available.');
      return;
    }
    this.itemName = last.itemName;
    this.amount = Number(last.amount);
    this.priority = last.priority || this.priority;
    await this.check();
  }
  async remove(row: AffordCheck): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this check?')) await this.vault.deleteAfford(row.id);
  }
  async createGoal(): Promise<void> {
    const row = this.result();
    if (!row || row.level === 'success') return;
    await this.router.navigate(['/goals'], { queryParams: { title: row.itemName, amount: row.amount } });
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every affordability check?')) await this.vault.clearAfford();
  }
}
