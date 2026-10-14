import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { FinanceService } from '../../../service/finance.service';
import { parseApiError } from '../../../shared/api-error';
import { API_CAPABILITIES, categoryRemaining, goalProgress } from '../../../shared/finance-rules';
import { formatINR, num } from '../../../shared/format';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './planning.component.html',
  styleUrl: './planning.component.scss',
})
export class PlanningComponent implements OnInit {
  private readonly auth = inject(AuthService);
  readonly vault = inject(FinanceService);
  readonly money = formatINR;
  readonly capabilities = API_CAPABILITIES;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly summary = signal<Record<string, unknown> | null>(null);

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  async load(): Promise<void> {
    const userId = this.auth.user()?.id;
    if (!userId) {
      this.error.set('Sign in again to load the monthly summary.');
      return;
    }
    if (!this.vault.finance()) {
      this.summary.set(null);
      this.error.set('No saved financial profile is available, so the monthly summary cannot be loaded.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    try {
      const row = await this.vault.monthlySummary(userId);
      this.summary.set(row);
      this.vault.acknowledgePlan();
    } catch (error) {
      this.summary.set(null);
      this.error.set(parseApiError(error).message);
    } finally {
      this.loading.set(false);
    }
  }

  insights(): string[] {
    const row = this.summary();
    const notes = row?.['insights'];
    return Array.isArray(notes) ? notes.map((item) => String(item)) : [];
  }

  surplus(): number | null {
    const row = this.summary();
    if (!row || row['monthlySurplus'] == null) return null;
    return num(row['monthlySurplus']);
  }

  goalRows() {
    return this.vault.goals().map((goal) => ({ ...goal, ...goalProgress(num(goal.saved), num(goal.target)) }));
  }

  budgetRows() {
    return this.vault.budgets().map((row) => ({ ...row, math: categoryRemaining(num(row.limit), num(row.spent)) }));
  }
}
