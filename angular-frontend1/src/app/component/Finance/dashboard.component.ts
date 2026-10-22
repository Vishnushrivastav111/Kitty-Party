import { Component, computed, effect, ElementRef, inject, signal, untracked, viewChild } from '@angular/core';
import { parseApiError } from '../../shared/api-error';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { ChartConfiguration } from 'chart.js';
import { destroyChart, mountChart } from '../../shared/charts';
import { AuthService } from '../../service/auth.service';
import { CHART_COLORS, formatDate, formatINR, num, queryRows } from '../../shared/format';
import {
  budgetComparison,
  categoryRemaining,
  dashboardGraphs,
  expenseBreakdown,
  goalProgress,
  goalSeries,
  incomeExpenseSeries,
  savingsTrend,
  stepGraph,
} from '../../shared/finance-rules';
import { FinanceService } from '../../service/finance.service';
import { GraphPagerComponent } from '../../shared/graph-pager.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent, NgClass, GraphPagerComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);
  readonly vault = inject(FinanceService);
  readonly prompt = signal(false);
  readonly search = signal('');
  readonly type = signal('');
  readonly pageNo = signal(1);
  readonly graphIndex = signal(0);
  readonly money = formatINR;
  readonly num = num;
  readonly date = formatDate;

  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');

  readonly name = computed(() => this.auth.user()?.fullName || this.vault.stats()?.fullName || 'Member');
  readonly stats = computed(() => this.vault.stats());
  /** Income minus category budgets and active goal plans. */
  readonly capacity = this.vault.capacity;
  readonly position = this.vault.position;
  /** Total savings come from the single savings overview; entries alone are only a fallback. */
  readonly totalSavings = computed(() => this.vault.savingsOverview()?.totalSavings ?? this.position().totalSavings);
  readonly budgetPct = computed(() => {
    const budget = this.position().monthlyBudget;
    if (!budget) return 0;
    return Math.min(100, Math.round((this.position().monthlySpending / budget) * 100));
  });
  readonly saveWidth = computed(() => Math.min(100, this.totalSavings() / 2000));
  readonly goalWidth = computed(() => Math.min(100, num(this.stats()?.activeGoals) * 20));
  readonly page = computed(() => queryRows(this.vault.transactions(), this.search(), ['name', 'category', 'type', 'date'], { type: this.type() }, this.pageNo(), 5));
  readonly goalPreview = computed(() => this.vault.goals().filter((goal) => (goal.status || '').toLowerCase() === 'active').slice(0, 4));
  readonly graphs = computed(() => dashboardGraphs({
    expenseCount: this.vault.transactions().filter((row) => row.type === 'expense').length,
    hasStats: !!this.stats(),
    savingsCount: this.vault.savings().length,
    budgetCount: this.vault.budgets().length,
    goalCount: this.vault.goals().length,
  }));
  readonly activeGraph = computed(() => this.graphs()[Math.min(this.graphIndex(), this.graphs().length - 1)]);

  /** Monthly plan: the confirmed monthly summary, shown on the dashboard instead of a separate planning page. */
  readonly plan = signal<Record<string, unknown> | null>(null);
  readonly planLoading = signal(false);
  readonly planError = signal('');
  readonly insights = computed(() => {
    const notes = this.plan()?.['insights'];
    return Array.isArray(notes) ? notes.map((item) => String(item)) : [];
  });
  readonly budgetRows = computed(() => this.vault.budgets().map((row) => ({ ...row, math: categoryRemaining(num(row.limit), num(row.spent)) })));

  constructor() {
    if (this.vault.needsSetup()) setTimeout(() => this.prompt.set(true), 250);
    effect(() => this.paint());
    void this.loadPlan();
    effect(() => {
      if (this.vault.planStale()) untracked(() => void this.loadPlan());
    });
  }

  async loadPlan(): Promise<void> {
    const userId = this.auth.user()?.id;
    if (!userId || !this.vault.finance()) {
      this.plan.set(null);
      this.planError.set('');
      return;
    }
    this.planLoading.set(true);
    this.planError.set('');
    try {
      this.plan.set(await this.vault.monthlySummary(userId));
      this.vault.acknowledgePlan();
    } catch (error) {
      this.plan.set(null);
      this.planError.set(parseApiError(error).message);
    } finally {
      this.planLoading.set(false);
    }
  }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, this.graphs().length, 0));
  }

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setType(value: string): void { this.type.set(value); this.pageNo.set(1); }
  pct(goal: { saved: number; target: number }): number {
    return goalProgress(num(goal.saved), num(goal.target)).percent;
  }

  private paint(): void {
    const graph = this.activeGraph();
    const canvas = this.stage()?.nativeElement;
    const stats = this.stats();
    const txs = this.vault.transactions();
    const savings = this.vault.savings();
    const budgets = this.vault.budgets();
    const goals = this.vault.goals();
    if (!graph?.hasData || !canvas) {
      destroyChart(canvas);
      return;
    }
    mountChart(canvas, this.configFor(graph.id, stats, txs, savings, budgets, goals));
  }

  private configFor(
    id: string,
    stats: { income: number; expenses: number; health: number; monthBudget: number; spent: number } | null,
    txs: Array<{ category: string; type: string; amount: number }>,
    savings: Array<{ amount: number; date: string }>,
    budgets: Array<{ category: string; limit: number; spent?: number }>,
    goals: Array<{ title: string; saved: number; target: number }>,
  ): ChartConfiguration {
    const colors = CHART_COLORS;
    if (id === 'expense') {
      const series = expenseBreakdown(txs)!;
      return { type: 'pie', data: { labels: series.labels, datasets: [{ data: series.data, backgroundColor: colors, borderWidth: 0 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } };
    }
    if (id === 'income') {
      const series = incomeExpenseSeries(stats)!;
      return { type: 'bar', data: { labels: series.labels, datasets: [{ label: 'Amount (₹)', data: series.data, backgroundColor: [colors[0], colors[3]], borderRadius: 8 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } };
    }
    if (id === 'savings') {
      const series = savingsTrend([...savings].reverse())!;
      return { type: 'line', data: { labels: series.labels.map((label) => formatDate(label)), datasets: [{ label: 'Recorded savings entries', data: series.data, borderColor: colors[0], backgroundColor: 'rgba(13,148,136,0.15)', fill: true, tension: 0.35 }] }, options: { responsive: true, maintainAspectRatio: false } };
    }
    if (id === 'budget') {
      const series = budgetComparison(budgets)!;
      return { type: 'bar', data: { labels: series.labels, datasets: [{ label: 'Limit', data: series.limit, backgroundColor: colors[1], borderRadius: 6 }, { label: 'Spent', data: series.spent, backgroundColor: colors[2], borderRadius: 6 }] }, options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } } };
    }
    if (id === 'health') {
      const score = Math.min(100, Math.max(0, num(stats!.health)));
      return { type: 'doughnut', data: { labels: ['Health score', 'Remaining to 100'], datasets: [{ data: [score, 100 - score], backgroundColor: [colors[5], '#e2e8f0'], borderWidth: 0 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } };
    }
    const series = goalSeries(goals)!;
    return { type: 'bar', data: { labels: series.labels, datasets: [{ label: 'Saved', data: series.saved, backgroundColor: colors[0], borderRadius: 6 }, { label: 'Target', data: series.target, backgroundColor: colors[1], borderRadius: 6 }] }, options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } } };
  }
}
