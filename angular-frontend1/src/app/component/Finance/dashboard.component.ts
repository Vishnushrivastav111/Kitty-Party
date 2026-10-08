import { NgClass } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { mountChart } from '../../shared/charts';
import { AuthService } from '../../service/auth.service';
import { CHART_COLORS, formatDate, formatINR, num, queryRows } from '../../shared/format';
import { FinanceService } from '../../service/finance.service';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent, NgClass],
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
  readonly money = formatINR;
  readonly date = formatDate;

  private readonly pie = viewChild<ElementRef<HTMLCanvasElement>>('expensePie');
  private readonly bar = viewChild<ElementRef<HTMLCanvasElement>>('incomeBar');
  private readonly line = viewChild<ElementRef<HTMLCanvasElement>>('savingsLine');
  private readonly doughnut = viewChild<ElementRef<HTMLCanvasElement>>('healthDoughnut');

  readonly name = computed(() => this.auth.user()?.fullName || this.vault.stats()?.fullName || 'Member');
  readonly stats = computed(() => this.vault.stats() || {
    health: 0, totalSavings: 0, monthBudget: 0, spent: 0, activeGoals: 0, income: 0, expenses: 0,
  });
  readonly budgetPct = computed(() => this.stats().monthBudget ? Math.min(100, Math.round((this.stats().spent / this.stats().monthBudget) * 100)) : 0);
  readonly saveWidth = computed(() => Math.min(100, this.stats().totalSavings / 2000));
  readonly goalWidth = computed(() => Math.min(100, this.stats().activeGoals * 20));
  readonly page = computed(() => queryRows(this.vault.transactions(), this.search(), ['name', 'category', 'type', 'date'], { type: this.type() }, this.pageNo(), 5));
  readonly goalPreview = computed(() => this.vault.goals().slice(0, 4));

  constructor() {
    if (this.vault.needsSetup()) setTimeout(() => this.prompt.set(true), 250);
    effect(() => {
      const stats = this.stats();
      const txs = this.vault.transactions();
      const savings = this.vault.savings();
      const pie = this.pie()?.nativeElement;
      const bar = this.bar()?.nativeElement;
      const line = this.line()?.nativeElement;
      const doughnut = this.doughnut()?.nativeElement;
      if (!pie || !bar || !line || !doughnut) return;
      const colors = CHART_COLORS;
      const byCat: Record<string, number> = {};
      txs.filter((row) => row.type === 'expense').forEach((row) => {
        byCat[row.category] = (byCat[row.category] || 0) + num(row.amount);
      });
      const labels = Object.keys(byCat);
      mountChart(pie, {
        type: 'pie',
        data: { labels: labels.length ? labels : ['No data'], datasets: [{ data: labels.length ? Object.values(byCat) : [1], backgroundColor: colors, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
      mountChart(bar, {
        type: 'bar',
        data: {
          labels: ['Income', 'Expense', 'Savings', 'Budget'],
          datasets: [{ label: 'Amount (₹)', data: [stats.income, stats.spent, stats.totalSavings, stats.monthBudget], backgroundColor: [colors[0], colors[3], colors[5], colors[1]], borderRadius: 8 }],
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
      });
      let running = 0;
      const lineLabels: string[] = [];
      const lineData: number[] = [];
      [...savings].reverse().forEach((row) => {
        running += num(row.amount);
        lineLabels.push(formatDate(row.date));
        lineData.push(running);
      });
      mountChart(line, {
        type: 'line',
        data: {
          labels: lineLabels.length ? lineLabels : ['Start'],
          datasets: [{ label: 'Vault balance', data: lineData.length ? lineData : [0], borderColor: colors[0], backgroundColor: 'rgba(13,148,136,0.15)', fill: true, tension: 0.35, pointBackgroundColor: colors[1] }],
        },
        options: { responsive: true, maintainAspectRatio: false },
      });
      const budgetPct = this.budgetPct();
      mountChart(doughnut, {
        type: 'doughnut',
        data: {
          labels: ['Health score', 'Remaining', 'Budget used %', 'Unused budget %'],
          datasets: [{ data: [stats.health, 100 - stats.health, budgetPct, Math.max(0, 100 - budgetPct)], backgroundColor: [colors[5], '#e2e8f0', colors[2], colors[7]], borderWidth: 0 }],
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
    });
  }

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setType(value: string): void { this.type.set(value); this.pageNo.set(1); }
  pct(goal: { saved: number; target: number }): number {
    return Math.min(100, Math.round((num(goal.saved) / (num(goal.target) || 1)) * 100));
  }
}
