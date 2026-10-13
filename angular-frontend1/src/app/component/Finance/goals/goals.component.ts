import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { destroyChart, mountChart } from '../../../shared/charts';
import { DialogService } from '../../../shared/dialog.service';
import { parseApiError } from '../../../shared/api-error';
import { assignError } from '../../../shared/field-state';
import { goalProgress, goalSeries, stepGraph } from '../../../shared/finance-rules';
import { CHART_COLORS, GOAL_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { Goal } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { GraphPagerComponent } from '../../../shared/graph-pager.component';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [FormsModule, PagerComponent, GraphPagerComponent],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent {
  private readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');
  readonly categories = GOAL_CATEGORIES;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  readonly graphIndex = signal(0);
  readonly graphPages = computed(() => [
    { title: 'Saved vs target', hasData: this.vault.goals().length > 0, empty: 'No goals are available for this graph.' },
    { title: 'Status mix', hasData: this.vault.goals().length > 0, empty: 'No goals are available for this graph.' },
  ]);
  readonly activeGraph = computed(() => this.graphPages()[Math.min(this.graphIndex(), 1)]);
  readonly search = signal('');
  readonly status = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form = blank();
  readonly page = computed(() => queryRows(this.vault.goals(), this.search(), ['title', 'category', 'status'], { status: this.status(), category: this.category() }, this.pageNo(), 6));

  constructor() {
    effect(() => this.paint());
  }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, this.graphPages().length, 0));
  }

  private paint(): void {
    const graph = this.activeGraph();
    const canvas = this.stage()?.nativeElement;
    const goals = this.vault.goals();
    if (!graph.hasData || !canvas) {
      destroyChart(canvas);
      return;
    }
    if (this.graphIndex() === 0) {
      const series = goalSeries(goals);
      if (!series) return;
      mountChart(canvas, {
        type: 'bar',
        data: {
          labels: series.labels,
          datasets: [
            { label: 'Saved', data: series.saved, backgroundColor: CHART_COLORS[0], borderRadius: 6 },
            { label: 'Target', data: series.target, backgroundColor: CHART_COLORS[1], borderRadius: 6 },
          ],
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
      });
      return;
    }
    const mix: Record<string, number> = {};
    goals.forEach((goal) => { mix[goal.status || 'active'] = (mix[goal.status || 'active'] || 0) + 1; });
    mountChart(canvas, {
      type: 'pie',
      data: { labels: Object.keys(mix), datasets: [{ data: Object.values(mix), backgroundColor: CHART_COLORS, borderWidth: 0 }] },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
    });
  }

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setFilter(key: 'status' | 'category', value: string): void {
    if (key === 'status') this.status.set(value); else this.category.set(value);
    this.pageNo.set(1);
  }
  pct(row: Goal): number { return goalProgress(num(row.saved), num(row.target)).percent; }
  remaining(row: Goal): number { return goalProgress(num(row.saved), num(row.target)).remaining; }
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  requiredTitle(value: string): string { return Validators.required(value, 'Title'); }
  amountOk(value: unknown): string { return Validators.amount(value, 'Saved amount'); }
  deadlineOk(value: string): string { return Validators.dateNotPast(value || '', 'Deadline'); }
  targetOk(value: unknown): string { return Number(value) > 0 ? '' : 'Target must be greater than zero'; }
  open(row: Goal | null): void {
    this.form = row ? { ...row, target: num(row.target), saved: num(row.saved), deadline: (row.deadline || '').slice(0, 10) } : blank();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = {
      title: Validators.required(this.form.title, 'Title'),
      category: Validators.required(this.form.category, 'Category'),
      target: Number(this.form.target) > 0 ? '' : 'Target must be greater than zero',
      saved: Validators.amount(this.form.saved, 'Saved amount'),
      deadline: Validators.dateNotPast(this.form.deadline || '', 'Deadline'),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { title: this.form.title.trim(), category: this.form.category, target: Number(this.form.target), saved: Number(this.form.saved), deadline: this.form.deadline, status: this.form.status };
    this.busy.set(true);
    try {
      const warning = this.form.id ? await this.vault.updateGoal(this.form.id, body) : await this.vault.addGoal(body);
      this.editing.set(false);
      if (warning) await this.dialog.notice(warning, 'Saved, refresh failed');
    } catch (error) {
      const parsed = parseApiError(error);
      this.errors.set({ ...errors, ...parsed.fieldErrors, form: parsed.message });
    } finally { this.busy.set(false); }
  }
  async remove(row: Goal): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this goal?')) await this.vault.deleteGoal(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every goal? This action cannot be undone.')) await this.vault.clearGoals();
  }
}

function blank(): Goal {
  return { id: '', title: '', category: '', target: 0, saved: 0, deadline: todayISO(), status: 'active' };
}
