import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { destroyChart, mountChart } from '../../../shared/charts';
import { budgetComparison, categoryRemaining, roundMoney, stepGraph } from '../../../shared/finance-rules';
import { GraphPagerComponent } from '../../../shared/graph-pager.component';
import { parseApiError } from '../../../shared/api-error';
import { DialogService } from '../../../shared/dialog.service';
import { assignError } from '../../../shared/field-state';
import { BUDGET_CATEGORIES, CHART_COLORS, Validators, formatINR, num, queryRows } from '../../../shared/format';
import { Budget } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent, GraphPagerComponent],
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.scss',
})
export class BudgetComponent {
  readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');
  readonly graphIndex = signal(0);
  readonly categories = BUDGET_CATEGORIES;
  readonly money = formatINR;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: Budget = { id: '', category: '', limit: 0, note: '' };
  readonly totals = computed(() => {
    const sum = this.vault.budgets().reduce((acc, row) => ({
      limit: roundMoney(acc.limit + num(row.limit)),
      spent: roundMoney(acc.spent + num(row.spent)),
    }), { limit: 0, spent: 0 });
    return { ...sum, ...categoryRemaining(sum.limit, sum.spent) };
  });
  readonly page = computed(() => queryRows(this.vault.budgets(), this.search(), ['category', 'note'], { status: this.status() }, this.pageNo(), 6));

  constructor() {
    effect(() => this.paint());
  }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, 2, 0));
  }

  private paint(): void {
    const canvas = this.stage()?.nativeElement;
    const rows = this.vault.budgets();
    if (!rows.length || !canvas) {
      destroyChart(canvas);
      return;
    }
    if (this.graphIndex() === 0) {
      const series = budgetComparison(rows)!;
      mountChart(canvas, {
        type: 'bar',
        data: {
          labels: series.labels,
          datasets: [
            { label: 'Limit', data: series.limit, backgroundColor: CHART_COLORS[1], borderRadius: 6 },
            { label: 'Spent', data: series.spent, backgroundColor: CHART_COLORS[2], borderRadius: 6 },
          ],
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
      });
      return;
    }
    mountChart(canvas, {
      type: 'pie',
      data: { labels: rows.map((row) => row.category), datasets: [{ data: rows.map((row) => num(row.limit)), backgroundColor: CHART_COLORS, borderWidth: 0 }] },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
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
      const warning = this.form.id ? await this.vault.updateBudget(this.form.id, body) : await this.vault.addBudget(body);
      this.editing.set(false);
      if (warning) await this.dialog.notice(warning, 'Saved, refresh failed');
    } catch (error) {
      const parsed = parseApiError(error);
      this.errors.set({ ...errors, ...parsed.fieldErrors, form: parsed.message });
    } finally { this.busy.set(false); }
  }
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  limitOk(value: unknown): string { return Number(value) > 0 ? '' : 'Limit must be greater than zero'; }
  async remove(row: Budget): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this budget?')) await this.vault.deleteBudget(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every budget? This action cannot be undone.')) await this.vault.clearBudgets();
  }
}
