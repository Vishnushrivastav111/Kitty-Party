import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { DialogService } from '../../../shared/dialog.service';
import { parseApiError } from '../../../shared/api-error';
import { assignError } from '../../../shared/field-state';
import { asStringList, formatINR, homePath, num, ROLES, todayISO, Validators } from '../../../shared/format';
import { FinanceService } from '../../../service/finance.service';

/**
 * Income-first setup. Only the monthly take-home income is required.
 * Expenses, loans and EMI are planned as category budgets in the Budget section.
 */
@Component({
  selector: 'app-setup',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './setup.component.html',
  styleUrl: './setup.component.scss',
})
export class SetupComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly router = inject(Router);
  readonly today = todayISO();
  readonly money = formatINR;
  readonly num = num;
  readonly investOptions = ['Mutual Funds', 'Stocks', 'Gold', 'PPF/EPF', 'Real Estate', 'Crypto', 'Other'];
  readonly investTypes = new Set<string>();
  incomeSource = '';
  monthlyIncome: number | null = null;
  additionalIncome: number | null = null;
  payCycle = '';
  currentSavings: number | null = null;
  emergencyReserve: number | null = null;
  savingsType = '';
  investments: number | null = null;
  hasGoal = 'no';
  primaryGoal = '';
  goalCategory = '';
  goalTarget = 0;
  goalDate = todayISO();
  confirmAccurate = false;
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly savedState = signal('');

  async ngOnInit(): Promise<void> {
    if (this.auth.user()?.role !== ROLES.USER) {
      await this.router.navigateByUrl(homePath(this.auth.user()?.role));
      return;
    }
    if (!this.vault.loaded()) {
      try { await this.vault.load(); } catch { /* form still usable */ }
    }
    const existing = this.vault.finance();
    if (!existing) return;
    this.incomeSource = existing.incomeSource || '';
    this.monthlyIncome = num(existing.monthlyIncome) || null;
    this.additionalIncome = num(existing.additionalIncome) || null;
    this.payCycle = existing.payCycle || '';
    this.currentSavings = num(existing.currentSavings) || null;
    this.emergencyReserve = num(existing.emergencyReserve) || null;
    this.savingsType = existing.savingsType || '';
    this.investments = num(existing.investments) || null;
    asStringList(existing.investmentTypes).forEach((item) => this.investTypes.add(item));
  }

  toggle(set: Set<string>, value: string): void {
    if (set.has(value)) set.delete(value); else set.add(value);
  }

  totalIncome(): number {
    return num(this.monthlyIncome) + num(this.additionalIncome);
  }

  touch(field: string, message: string): void {
    this.errors.update((current) => assignError(current, field, message));
  }
  incomeOk(value: unknown): string {
    const base = Validators.amount(value, 'Monthly income');
    if (base) return base;
    return Number(value) > 0 ? '' : 'Monthly income must be greater than zero';
  }
  optionalAmountOk(value: unknown, label: string): string {
    if (value === '' || value === null || value === undefined) return '';
    return Validators.amount(value, label);
  }
  reserveOk(): string {
    return num(this.emergencyReserve) > num(this.currentSavings) ? 'Emergency reserve cannot be larger than liquid savings' : '';
  }
  choiceOk(value: string): string { return value ? '' : 'This field is required'; }
  goalTitleOk(value: string): string { return Validators.required(value, 'Goal title'); }
  goalDateOk(value: string): string { return Validators.dateNotPast(value, 'Deadline'); }
  goalTargetOk(value: unknown): string { return Number(value) > 0 ? '' : 'Target must be greater than zero'; }

  async skip(): Promise<void> {
    await this.vault.skipSetup();
    await this.router.navigateByUrl('/dashboard');
  }

  async submit(): Promise<void> {
    const errors: Record<string, string> = {
      monthlyIncome: this.incomeOk(this.monthlyIncome),
      additionalIncome: this.optionalAmountOk(this.additionalIncome, 'Other income'),
      currentSavings: this.optionalAmountOk(this.currentSavings, 'Current savings'),
      investments: this.optionalAmountOk(this.investments, 'Investments'),
      emergencyReserve: this.optionalAmountOk(this.emergencyReserve, 'Emergency reserve') || this.reserveOk(),
      confirm: this.confirmAccurate ? '' : 'Please confirm accuracy',
    };
    if (this.hasGoal === 'yes') {
      errors['primaryGoal'] = Validators.required(this.primaryGoal, 'Goal title');
      errors['goalCategory'] = this.goalCategory ? '' : 'Select category';
      errors['goalTarget'] = Number(this.goalTarget) > 0 ? '' : 'Target must be greater than zero';
      errors['goalDate'] = Validators.dateNotPast(this.goalDate, 'Deadline');
    }
    this.errors.set(errors);
    this.formError.set('');
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      await this.vault.saveFinance({
        incomeSource: this.incomeSource,
        monthlyIncome: num(this.monthlyIncome),
        additionalIncome: num(this.additionalIncome),
        payCycle: this.payCycle,
        currentSavings: num(this.currentSavings),
        emergencyReserve: num(this.emergencyReserve),
        savingsType: this.savingsType,
        investments: num(this.investments),
        investmentTypes: [...this.investTypes],
        setupDate: this.today,
      });
      const followUp: string[] = [];
      if (this.hasGoal === 'yes' && this.primaryGoal.trim()) {
        try {
          await this.vault.addGoal({
            title: this.primaryGoal.trim(),
            target: num(this.goalTarget) || 1,
            saved: 0,
            deadline: this.goalDate,
            status: 'draft',
            category: this.goalCategory || 'Personal',
          });
        } catch (error) {
          followUp.push(parseApiError(error).message);
        }
      }
      const needsBudgets = this.vault.budgets().length === 0;
      this.savedState.set('Financial profile saved.');
      const message = needsBudgets
        ? 'Your income is saved. Next, add category budgets (rent, food, EMI / Loans and others) in the Budget section. Together with your goal contributions they are checked against your income.'
        : '';
      if (followUp.length || message) await this.dialog.notice([...followUp, message].filter(Boolean).join(' '), 'Profile saved');
      await this.router.navigateByUrl(needsBudgets ? '/budget' : '/dashboard');
    } catch (error) {
      const parsed = parseApiError(error);
      this.formError.set(parsed.message);
      this.errors.set({ ...errors, ...parsed.fieldErrors });
      this.savedState.set('');
    } finally { this.busy.set(false); }
  }
}
