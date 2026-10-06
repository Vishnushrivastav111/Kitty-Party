import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DialogService } from '../core/dialog.service';
import { asStringList, formatINR, homePath, num, ROLES, todayISO, Validators } from '../core/format';
import { VaultService } from '../core/vault.service';

@Component({
  selector: 'app-setup',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './setup.component.css',
  template: `
    <div class="container">
      <div class="setup-brand"><i class="fas fa-vault"></i> MicroVault</div>
      <h2>Personal Financial Setup</h2>
      <p class="subtitle">Complete your profile — income, expenses, loans, savings, and budget</p>
      <form (ngSubmit)="submit()" novalidate>
        <h3>1. Income Details</h3>
        <div class="form-group"><label>Income Source</label>
          <select name="incomeSource" [(ngModel)]="incomeSource"><option value="">Select income source</option><option>Salary</option><option>Business</option><option>Freelance</option><option>Investments</option><option>Other</option></select>
          <span class="field-error" [style.display]="errors()['incomeSource'] ? 'block' : 'none'">{{ errors()['incomeSource'] }}</span>
        </div>
        <div class="form-group"><label>Monthly Income (₹)</label><input type="number" name="monthlyIncome" min="0" [(ngModel)]="monthlyIncome" /><span class="field-error" [style.display]="errors()['monthlyIncome'] ? 'block' : 'none'">{{ errors()['monthlyIncome'] }}</span></div>
        <div class="form-group"><label>Pay Cycle</label>
          <select name="payCycle" [(ngModel)]="payCycle"><option value="">Select pay cycle</option><option>Monthly</option><option>Bi-weekly</option><option>Weekly</option><option>Irregular</option></select>
          <span class="field-error" [style.display]="errors()['payCycle'] ? 'block' : 'none'">{{ errors()['payCycle'] }}</span>
        </div>
        <h3>2. Monthly Expenses</h3>
        <div class="form-group"><label>Total Monthly Expenses (₹)</label><input type="number" name="monthlyExpenses" min="0" [(ngModel)]="monthlyExpenses" /><span class="field-error" [style.display]="errors()['monthlyExpenses'] ? 'block' : 'none'">{{ errors()['monthlyExpenses'] }}</span></div>
        <label>Expense Categories (select all that apply)</label>
        <div class="check-row">
          @for (item of expenseOptions; track item) {
            <label><input type="checkbox" [checked]="expenseCats.has(item)" (change)="toggle(expenseCats, item)" /> {{ item }}</label>
          }
        </div>
        <span class="field-error" [style.display]="errors()['expenseCats'] ? 'block' : 'none'">{{ errors()['expenseCats'] }}</span>
        <h3>3. Loans / EMI</h3>
        <div class="radio-group">
          <span style="font-weight:600">Do you have any loans?</span>
          <label><input type="radio" name="hasLoan" value="yes" [(ngModel)]="hasLoan" /> Yes</label>
          <label><input type="radio" name="hasLoan" value="no" [(ngModel)]="hasLoan" /> No</label>
        </div>
        @if (hasLoan === 'yes') {
          <div class="form-group"><label>Loan Type</label>
            <select name="loanType" [(ngModel)]="loanType"><option value="">Select loan type</option><option>Home Loan</option><option>Car Loan</option><option>Personal Loan</option><option>Education Loan</option><option>Credit Card</option><option>Other</option></select>
            <span class="field-error" [style.display]="errors()['loanType'] ? 'block' : 'none'">{{ errors()['loanType'] }}</span>
          </div>
          <div class="form-group"><label>Outstanding Loan Amount (₹)</label><input type="number" name="loanAmount" min="0" [(ngModel)]="loanAmount" /></div>
          <div class="radio-group">
            <span style="font-weight:600">Paying EMI currently?</span>
            <label><input type="radio" name="hasEmi" value="yes" [(ngModel)]="hasEmi" /> Yes</label>
            <label><input type="radio" name="hasEmi" value="no" [(ngModel)]="hasEmi" /> No</label>
          </div>
          @if (hasEmi === 'yes') {
            <div class="form-group"><label>Monthly EMI (₹)</label><input type="number" name="loansEmi" min="0" [(ngModel)]="loansEmi" /><span class="field-error" [style.display]="errors()['loansEmi'] ? 'block' : 'none'">{{ errors()['loansEmi'] }}</span></div>
            <div class="form-group"><label>EMI Start Date</label><input type="date" name="emiStartDate" [(ngModel)]="emiStartDate" [max]="today" /><span class="field-error" [style.display]="errors()['emiStartDate'] ? 'block' : 'none'">{{ errors()['emiStartDate'] }}</span></div>
          }
        }
        <h3>4. Savings &amp; Investments</h3>
        <div class="form-group"><label>Current Savings (₹)</label><input type="number" name="currentSavings" min="0" [(ngModel)]="currentSavings" /><span class="field-error" [style.display]="errors()['currentSavings'] ? 'block' : 'none'">{{ errors()['currentSavings'] }}</span></div>
        <div class="form-group"><label>Primary Savings Type</label>
          <select name="savingsType" [(ngModel)]="savingsType"><option value="">Select</option><option>Bank Savings</option><option>Fixed Deposit</option><option>Recurring Deposit</option><option>Cash</option><option>Other</option></select>
          <span class="field-error" [style.display]="errors()['savingsType'] ? 'block' : 'none'">{{ errors()['savingsType'] }}</span>
        </div>
        <div class="form-group"><label>Total Investments (₹)</label><input type="number" name="investments" min="0" [(ngModel)]="investments" /></div>
        <label>Investment Types (select all that apply)</label>
        <div class="check-row">
          @for (item of investOptions; track item) {
            <label><input type="checkbox" [checked]="investTypes.has(item)" (change)="toggle(investTypes, item)" /> {{ item }}</label>
          }
        </div>
        <h3>5. Monthly Budget</h3>
        <div class="form-group"><label>Monthly Budget Limit (₹)</label><input type="number" name="monthlyBudget" min="0" [(ngModel)]="monthlyBudget" /><span class="field-error" [style.display]="errors()['monthlyBudget'] ? 'block' : 'none'">{{ errors()['monthlyBudget'] }}</span></div>
        <div class="form-group"><label>Budget Style</label>
          <select name="budgetStyle" [(ngModel)]="budgetStyle"><option value="">Select style</option><option>50/30/20 Rule</option><option>Zero-based</option><option>Envelope</option><option>Custom</option></select>
          <span class="field-error" [style.display]="errors()['budgetStyle'] ? 'block' : 'none'">{{ errors()['budgetStyle'] }}</span>
        </div>
        <h3>6. Financial Goal</h3>
        <div class="radio-group">
          <span style="font-weight:600">Do you want to set a goal now?</span>
          <label><input type="radio" name="hasGoal" value="yes" [(ngModel)]="hasGoal" /> Yes</label>
          <label><input type="radio" name="hasGoal" value="no" [(ngModel)]="hasGoal" /> No</label>
        </div>
        @if (hasGoal === 'yes') {
          <div class="form-group"><label>Goal Title</label><input name="primaryGoal" [(ngModel)]="primaryGoal" /><span class="field-error" [style.display]="errors()['primaryGoal'] ? 'block' : 'none'">{{ errors()['primaryGoal'] }}</span></div>
          <div class="form-group"><label>Goal Category</label>
            <select name="goalCategory" [(ngModel)]="goalCategory"><option value="">Select</option><option>Emergency</option><option>Travel</option><option>Education</option><option>Home</option><option>Personal</option><option>Other</option></select>
            <span class="field-error" [style.display]="errors()['goalCategory'] ? 'block' : 'none'">{{ errors()['goalCategory'] }}</span>
          </div>
          <div class="form-group"><label>Target Amount (₹)</label><input type="number" name="goalTarget" min="0" [(ngModel)]="goalTarget" /><span class="field-error" [style.display]="errors()['goalTarget'] ? 'block' : 'none'">{{ errors()['goalTarget'] }}</span></div>
          <div class="form-group"><label>Goal Start / As-of Date</label><input type="date" name="goalDate" [(ngModel)]="goalDate" [max]="today" /></div>
        }
        <h3>7. Planned Big Purchases</h3>
        <div class="radio-group">
          <span style="font-weight:600">Planning a big purchase?</span>
          <label><input type="radio" name="hasPurchase" value="yes" [(ngModel)]="hasPurchase" /> Yes</label>
          <label><input type="radio" name="hasPurchase" value="no" [(ngModel)]="hasPurchase" /> No</label>
        </div>
        @if (hasPurchase === 'yes') {
          <div class="form-group"><label>Purchase Item</label><input name="purchaseItem" [(ngModel)]="purchaseItem" /><span class="field-error" [style.display]="errors()['purchaseItem'] ? 'block' : 'none'">{{ errors()['purchaseItem'] }}</span></div>
          <div class="form-group"><label>Estimated Cost (₹)</label><input type="number" name="purchaseAmount" min="0" [(ngModel)]="purchaseAmount" /><span class="field-error" [style.display]="errors()['purchaseAmount'] ? 'block' : 'none'">{{ errors()['purchaseAmount'] }}</span></div>
          <div class="form-group"><label>Priority</label>
            <select name="purchasePriority" [(ngModel)]="purchasePriority"><option value="">Select</option><option>Need</option><option>Want</option><option>Investment</option></select>
            <span class="field-error" [style.display]="errors()['purchasePriority'] ? 'block' : 'none'">{{ errors()['purchasePriority'] }}</span>
          </div>
        }
        <div class="form-group"><label>Profile As-of Date</label><input type="date" name="setupDate" [(ngModel)]="setupDate" [max]="today" /><span class="field-error" [style.display]="errors()['setupDate'] ? 'block' : 'none'">{{ errors()['setupDate'] }}</span></div>
        <div class="summary-box">
          <div><span>Estimated surplus</span><strong>{{ money(surplus()) }}</strong></div>
          <div><span>Income</span><strong>{{ money(monthlyIncome) }}</strong></div>
          <div><span>Expenses + EMI</span><strong>{{ money(num(monthlyExpenses) + (hasEmi === 'yes' ? num(loansEmi) : 0)) }}</strong></div>
        </div>
        <label class="check-row"><input type="checkbox" name="confirm" [(ngModel)]="confirmAccurate" /> I confirm the financial information above is accurate</label>
        <span class="field-error" [style.display]="errors()['confirm'] ? 'block' : 'none'">{{ errors()['confirm'] }}</span>
        <button class="btn" [disabled]="busy()">Save &amp; Continue to Dashboard</button>
        <button type="button" class="btn skip" (click)="skip()">Skip for now</button>
      </form>
    </div>
  `,
})
export class SetupComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly vault = inject(VaultService);
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
