import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../../../shared/charts';
import { DialogService } from '../../../shared/dialog.service';
import { BUDGET_CATEGORIES, CHART_COLORS, Validators, formatINR, num, queryRows } from '../../../shared/format';
import { Budget } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.scss',
})
export class BudgetComponent {
  private readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly bar = viewChild<ElementRef<HTMLCanvasElement>>('budgetBar');
  private readonly pie = viewChild<ElementRef<HTMLCanvasElement>>('budgetPie');
  readonly categories = BUDGET_CATEGORIES;
  readonly money = formatINR;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: Budget = { id: '', category: '', limit: 0, note: '' };
  readonly totals = computed(() => this.vault.budgets().reduce((sum, row) => ({ limit: sum.limit + num(row.limit), spent: sum.spent + num(row.spent) }), { limit: 0, spent: 0 }));
  readonly page = computed(() => queryRows(this.vault.budgets(), this.search(), ['category', 'note'], { status: this.status() }, this.pageNo(), 6));

  constructor() {
    effect(() => {
      const rows = this.vault.budgets();
      const bar = this.bar()?.nativeElement;
      const pie = this.pie()?.nativeElement;
      if (!bar || !pie) return;
      const labels = rows.map((row) => row.category);
      mountChart(bar, {
        type: 'bar',
        data: {
          labels: labels.length ? labels : ['No budgets'],
          datasets: [
            { label: 'Limit', data: rows.length ? rows.map((row) => num(row.limit)) : [0], backgroundColor: CHART_COLORS[1], borderRadius: 6 },
            { label: 'Spent', data: rows.length ? rows.map((row) => num(row.spent)) : [0], backgroundColor: CHART_COLORS[2], borderRadius: 6 },
          ],
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
      });
      mountChart(pie, {
        type: 'pie',
        data: { labels: labels.length ? labels : ['No data'], datasets: [{ data: rows.length ? rows.map((row) => num(row.limit)) : [1], backgroundColor: CHART_COLORS, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
    });
  }

  label(status?: string): string {
    if (status === 'over') return 'Over budget';
    if (status === 'warn') return 'Near limit';
    return 'Within limit';
  }
  open(row: Budget | null): void {
    this.form = row ? { ...row, limit: num(row.limit), note: row.note || '' } : { id: '', category: '', limit: 0, note: '' };
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = { category: Validators.required(this.form.category, 'Category'), limit: Number(this.form.limit) > 0 ? '' : 'Limit must be greater than zero' };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { category: this.form.category, limit: Number(this.form.limit), note: this.form.note || '' };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateBudget(this.form.id, body); else await this.vault.addBudget(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save budget');
    } finally { this.busy.set(false); }
  }
  async remove(row: Budget): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this budget?')) await this.vault.deleteBudget(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every budget? This action cannot be undone.')) await this.vault.clearBudgets();
  }
}
