import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { DialogService } from '../../../shared/dialog.service';
import { asStringList, formatINR, homePath, num, ROLES, todayISO, Validators } from '../../../shared/format';
import { FinanceService } from '../../../service/finance.service';

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
  readonly expenseOptions = ['Rent/Housing', 'Food', 'Transport', 'Utilities', 'Healthcare', 'Entertainment', 'Education', 'Shopping', 'Other'];
  readonly investOptions = ['Mutual Funds', 'Stocks', 'Gold', 'PPF/EPF', 'Real Estate', 'Crypto', 'Other'];
  readonly expenseCats = new Set<string>();
  readonly investTypes = new Set<string>();
  incomeSource = '';
  monthlyIncome: number | null = null;
  payCycle = '';
  monthlyExpenses: number | null = null;
  hasLoan = 'no';
  loanType = '';
  loanAmount = 0;
  hasEmi = 'no';
  loansEmi = 0;
  emiStartDate = todayISO();
  currentSavings: number | null = null;
  savingsType = '';
  investments = 0;
  monthlyBudget: number | null = null;
  budgetStyle = '';
  hasGoal = 'no';
  primaryGoal = '';
  goalCategory = '';
  goalTarget = 0;
  goalDate = todayISO();
  hasPurchase = 'no';
  purchaseItem = '';
  purchaseAmount = 0;
  purchasePriority = '';
  setupDate = todayISO();
  confirmAccurate = false;
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});

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
    this.monthlyIncome = num(existing.monthlyIncome);
    this.payCycle = existing.payCycle || '';
    this.monthlyExpenses = num(existing.monthlyExpenses);
    this.loanType = existing.loanType || '';
    this.loanAmount = num(existing.loanAmount);
    this.loansEmi = num(existing.loansEmi || existing.monthlyEmi);
    this.emiStartDate = (existing.emiStartDate || this.today).slice(0, 10);
    this.currentSavings = num(existing.currentSavings);
    this.savingsType = existing.savingsType || '';
    this.investments = num(existing.investments);
    this.monthlyBudget = num(existing.monthlyBudget);
    this.budgetStyle = existing.budgetStyle || '';
    this.setupDate = (existing.setupDate || this.today).slice(0, 10);
    asStringList(existing.expenseCategories).forEach((item) => this.expenseCats.add(item));
    asStringList(existing.investmentTypes).forEach((item) => this.investTypes.add(item));
    const loan = existing.hasLoan;
    this.hasLoan = loan === true || loan === 'yes' ? 'yes' : 'no';
    this.hasEmi = this.loansEmi > 0 ? 'yes' : 'no';
  }

  toggle(set: Set<string>, value: string): void {
    if (set.has(value)) set.delete(value); else set.add(value);
  }

  surplus(): number {
    return num(this.monthlyIncome) - num(this.monthlyExpenses) - (this.hasEmi === 'yes' ? num(this.loansEmi) : 0);
  }

  async skip(): Promise<void> {
    await this.vault.skipSetup();
    await this.router.navigateByUrl('/dashboard');
  }

  async submit(): Promise<void> {
    const errors: Record<string, string> = {
      incomeSource: this.incomeSource ? '' : 'This field is required',
      payCycle: this.payCycle ? '' : 'This field is required',
      savingsType: this.savingsType ? '' : 'This field is required',
      budgetStyle: this.budgetStyle ? '' : 'This field is required',
      monthlyIncome: Validators.amount(this.monthlyIncome, 'Monthly income'),
      monthlyExpenses: Validators.amount(this.monthlyExpenses, 'Monthly expenses'),
      currentSavings: Validators.amount(this.currentSavings, 'Current savings'),
      monthlyBudget: Validators.amount(this.monthlyBudget, 'Monthly budget'),
      expenseCats: this.expenseCats.size ? '' : 'Select at least one expense category',
      setupDate: Validators.dateNotFuture(this.setupDate, 'As-of date'),
      confirm: this.confirmAccurate ? '' : 'Please confirm accuracy',
    };
    if (this.hasLoan === 'yes') errors['loanType'] = this.loanType ? '' : 'Select loan type';
    if (this.hasEmi === 'yes') {
      errors['loansEmi'] = Validators.amount(this.loansEmi, 'EMI');
      errors['emiStartDate'] = Validators.dateNotFuture(this.emiStartDate, 'EMI start date');
    }
    if (this.hasGoal === 'yes') {
      errors['primaryGoal'] = Validators.required(this.primaryGoal, 'Goal title');
      errors['goalCategory'] = this.goalCategory ? '' : 'Select category';
      errors['goalTarget'] = Number(this.goalTarget) > 0 ? '' : 'Target must be greater than zero';
    }
    if (this.hasPurchase === 'yes') {
      errors['purchaseItem'] = Validators.required(this.purchaseItem, 'Item');
      errors['purchaseAmount'] = Number(this.purchaseAmount) > 0 ? '' : 'Cost must be greater than zero';
      errors['purchasePriority'] = this.purchasePriority ? '' : 'Select priority';
    }
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      await this.vault.saveFinance({
        incomeSource: this.incomeSource,
        monthlyIncome: num(this.monthlyIncome),
        payCycle: this.payCycle,
        monthlyExpenses: num(this.monthlyExpenses),
        expenseCategories: [...this.expenseCats],
        hasLoan: this.hasLoan,
        loanType: this.loanType,
        loanAmount: num(this.loanAmount),
        loansEmi: this.hasEmi === 'yes' ? num(this.loansEmi) : 0,
        monthlyEmi: this.hasEmi === 'yes' ? num(this.loansEmi) : 0,
        emiStartDate: this.emiStartDate,
        currentSavings: num(this.currentSavings),
        savingsType: this.savingsType,
        investments: num(this.investments),
        investmentTypes: [...this.investTypes],
        monthlyBudget: num(this.monthlyBudget),
        budgetStyle: this.budgetStyle,
        setupDate: this.setupDate,
      });
      if (num(this.currentSavings) > 0 && !this.vault.savings().some((row) => row.title === 'Opening balance')) {
        await this.vault.addSaving({ title: 'Opening balance', amount: num(this.currentSavings), category: 'Opening', date: this.setupDate, note: 'From financial setup' });
      }
      if (this.hasGoal === 'yes' && this.primaryGoal.trim()) {
        await this.vault.addGoal({ title: this.primaryGoal.trim(), target: num(this.goalTarget) || 1, saved: 0, deadline: this.goalDate || this.setupDate, status: 'active', category: this.goalCategory || 'Personal' });
      }
      if (this.hasPurchase === 'yes' && this.purchaseItem.trim()) {
        await this.vault.checkAfford({ itemName: this.purchaseItem.trim(), amount: num(this.purchaseAmount) || 1, checkDate: this.setupDate, priority: this.purchasePriority || 'Want' });
      }
      await this.router.navigateByUrl('/dashboard');
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save your financial profile');
    } finally { this.busy.set(false); }
  }
}
