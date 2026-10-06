import { NgClass } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { mountChart } from '../core/charts';
import { AuthService } from '../core/auth.service';
import { CHART_COLORS, formatDate, formatINR, num, queryRows } from '../core/format';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent, NgClass],
  template: `
    @if (prompt()) {
      <div class="mv-confirm-backdrop open">
        <div class="mv-confirm-dialog" role="dialog" aria-modal="true">
          <div class="mv-confirm-icon info"><i class="fas fa-sliders"></i></div>
          <h3>Complete financial setup</h3>
          <p>You skipped Personal Financial Setup. Complete it to unlock dashboard data, charts, savings, and reports.</p>
          <div class="mv-confirm-actions">
            <button type="button" class="btn btn-outline" (click)="prompt.set(false)">Stay on dashboard</button>
            <a class="btn btn-primary" routerLink="/setup">Set up now</a>
          </div>
        </div>
      </div>
    }
    <div class="page-header">
      <h1>Welcome, <span>{{ name() }}</span></h1>
      <p>Live overview with charts, progress, and your saved data</p>
    </div>
    <div class="cards">
      <div class="card"><div class="card-icon navy"><i class="fas fa-heart-pulse"></i></div><div><h3>Financial health</h3><h2>{{ stats().health }}/100</h2><div class="progress c1"><span [style.width.%]="stats().health"></span></div></div></div>
      <div class="card"><div class="card-icon green"><i class="fas fa-piggy-bank"></i></div><div><h3>Total savings</h3><h2>{{ money(stats().totalSavings) }}</h2><div class="progress c6"><span [style.width.%]="saveWidth()"></span></div></div></div>
      <div class="card"><div class="card-icon orange"><i class="fas fa-chart-pie"></i></div><div><h3>Monthly budget</h3><h2>{{ money(stats().monthBudget) }}</h2><p [class.negative]="stats().spent > stats().monthBudget" [class.positive]="stats().spent <= stats().monthBudget">Spent {{ money(stats().spent) }}</p><div class="progress c3"><span [style.width.%]="budgetPct()"></span></div></div></div>
      <div class="card"><div class="card-icon teal"><i class="fas fa-bullseye"></i></div><div><h3>Active goals</h3><h2>{{ stats().activeGoals }}</h2><div class="progress c2"><span [style.width.%]="goalWidth()"></span></div></div></div>
    </div>
    <div class="charts-grid">
      <div class="chart-card"><h3><i class="fas fa-chart-pie"></i> Expense mix</h3><div class="chart-wrap"><canvas #expensePie></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-column"></i> Income vs expense</h3><div class="chart-wrap"><canvas #incomeBar></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-line"></i> Savings trend</h3><div class="chart-wrap"><canvas #savingsLine></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-gauge-high"></i> Health &amp; budget use</h3><div class="chart-wrap"><canvas #healthDoughnut></canvas></div></div>
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Recent transactions</h2>
        <a class="btn btn-primary btn-sm" routerLink="/transactions"><i class="fas fa-plus"></i> Manage</a>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search transactions..." [ngModel]="search()" (ngModelChange)="setSearch($event)" />
        <select [ngModel]="type()" (ngModelChange)="setType($event)">
          <option value="">All types</option>
          <option value="income">Income</option>
          <option value="expense">Expense</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Date</th><th>Name</th><th>Category</th><th>Type</th><th>Amount</th></tr></thead>
          <tbody>
            @if (!page().rows.length) {
              <tr><td colspan="5" class="empty-state">No transactions yet</td></tr>
            } @else {
              @for (row of page().rows; track row.id) {
                <tr>
                  <td>{{ date(row.date) }}</td>
                  <td>{{ row.name || '—' }}</td>
                  <td>{{ row.category || '—' }}</td>
                  <td><span class="badge" [class.badge-success]="row.type === 'income'" [class.badge-warning]="row.type !== 'income'">{{ row.type }}</span></td>
                  <td>{{ money(row.amount) }}</td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Active goals</h2>
        <a class="btn btn-outline btn-sm" routerLink="/goals">View all</a>
      </div>
      @if (!goalPreview().length) {
        <div class="empty-state">No goals yet. <a routerLink="/goals">Add a goal</a></div>
      } @else {
        @for (goal of goalPreview(); track goal.id; let i = $index) {
          <div style="margin-bottom:14px">
            <strong>{{ goal.title }}</strong> — {{ money(goal.saved) }} / {{ money(goal.target) }} ({{ pct(goal) }}%)
            <div class="progress" [ngClass]="'c' + ((i % 6) + 1)"><span [style.width.%]="pct(goal)"></span></div>
          </div>
        }
      }
    </div>
  `,
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);
  readonly vault = inject(VaultService);
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
