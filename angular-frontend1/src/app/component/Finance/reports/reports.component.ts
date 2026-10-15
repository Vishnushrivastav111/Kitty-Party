import { NgClass } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Report, Tx } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { parseApiError } from '../../../shared/api-error';
import { destroyChart, mountChart } from '../../../shared/charts';
import {
  ReportPreset,
  reportAnalysis,
  reportDateErrors,
  reportPresetRange,
  reportRequestBody,
  stepGraph,
  transactionsMatching,
} from '../../../shared/finance-rules';
import { CHART_COLORS, TX_CATEGORIES, formatDate, formatINR, monthStartISO, queryRows, todayISO } from '../../../shared/format';
import { GraphPagerComponent } from '../../../shared/graph-pager.component';
import { PagerComponent } from '../../../shared/pager.component';

const PRESETS: Array<{ id: ReportPreset; label: string }> = [
  { id: 'thisMonth', label: 'This month' },
  { id: 'lastMonth', label: 'Last month' },
  { id: 'last3Months', label: 'Last 3 months' },
  { id: 'thisYear', label: 'This year' },
  { id: 'custom', label: 'Custom dates' },
];

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [FormsModule, NgClass, RouterLink, PagerComponent, GraphPagerComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent {
  readonly vault = inject(FinanceService);
  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');

  readonly presets = PRESETS;
  readonly categories = TX_CATEGORIES;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;

  readonly preset = signal<ReportPreset>('thisMonth');
  readonly from = signal(monthStartISO());
  readonly to = signal(todayISO());
  readonly txType = signal('all');
  readonly category = signal('all');
  readonly reportName = signal('');
  readonly graphIndex = signal(0);
  readonly busy = signal(false);
  readonly formError = signal('');

  readonly search = signal('');
  readonly pageNo = signal(1);
  readonly historySearch = signal('');
  readonly historyPageNo = signal(1);

  readonly errors = computed(() => reportDateErrors(this.from(), this.to(), this.today));
  readonly valid = computed(() => !this.errors().from && !this.errors().to);
  readonly rows = computed(() => (this.valid() ? transactionsMatching(this.vault.transactions(), this.from(), this.to(), this.txType(), this.category()) : []));
  readonly analysis = computed(() => reportAnalysis(this.rows(), this.from(), this.to()));
  readonly presetLabel = computed(() => PRESETS.find((item) => item.id === this.preset())?.label || 'Custom dates');
  readonly periodTitle = computed(() => (this.valid() ? `${formatDate(this.from())} to ${formatDate(this.to())}` : 'Choose a valid period'));
  readonly page = computed(() => queryRows(this.rows(), this.search(), ['name', 'category', 'type', 'date'], {}, this.pageNo(), 8));
  readonly history = computed(() => queryRows(this.vault.reports(), this.historySearch(), ['type', 'fromDate', 'toDate'], {}, this.historyPageNo(), 6));
  readonly overview = this.vault.savingsOverview;

  constructor() {
    effect(() => this.paint());
  }

  choosePreset(preset: ReportPreset): void {
    this.preset.set(preset);
    const range = reportPresetRange(preset, this.today);
    if (range) {
      this.from.set(range.from);
      this.to.set(range.to);
    }
    this.pageNo.set(1);
    this.formError.set('');
  }

  setFrom(value: string): void { this.from.set(value); this.preset.set('custom'); this.pageNo.set(1); }
  setTo(value: string): void { this.to.set(value); this.preset.set('custom'); this.pageNo.set(1); }
  setType(value: string): void { this.txType.set(value); this.pageNo.set(1); }
  setCategory(value: string): void { this.category.set(value); this.pageNo.set(1); }
  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, 2, 0));
  }

  async save(): Promise<void> {
    this.formError.set('');
    if (!this.valid()) return;
    this.busy.set(true);
    try {
      await this.vault.generateReport(reportRequestBody(this.from(), this.to(), this.reportName().trim() || this.presetLabel()));
      this.reportName.set('');
    } catch (error) {
      this.formError.set(parseApiError(error).message);
    } finally {
      this.busy.set(false);
    }
  }

  open(row: Report): void {
    this.preset.set('custom');
    this.from.set(row.fromDate);
    this.to.set(row.toDate);
    this.txType.set('all');
    this.category.set('all');
    this.pageNo.set(1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  print(): void {
    window.print();
  }

  downloadCurrent(format: 'csv' | 'txt'): void {
    this.download(this.from(), this.to(), this.rows(), this.reportName().trim() || this.presetLabel(), format);
  }

  downloadSaved(row: Report, format: 'csv' | 'txt'): void {
    this.download(row.fromDate, row.toDate, transactionsMatching(this.vault.transactions(), row.fromDate, row.toDate, 'all', 'all'), row.type, format);
  }

  private download(from: string, to: string, rows: Tx[], title: string, format: 'csv' | 'txt'): void {
    const text = format === 'csv' ? csv(rows) : summaryText(from, to, rows, title);
    const blob = new Blob([format === 'csv' ? '\ufeff' + text : text], { type: format === 'csv' ? 'text/csv;charset=utf-8' : 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `MicroVault_Report_${from}_to_${to}.${format}`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private paint(): void {
    const canvas = this.stage()?.nativeElement;
    const view = this.analysis();
    if (!canvas || !this.valid() || !view.count) {
      destroyChart(canvas);
      return;
    }
    if (this.graphIndex() === 0) {
      if (!view.categories.length) { destroyChart(canvas); return; }
      mountChart(canvas, {
        type: 'doughnut',
        data: { labels: view.categories.map((item) => item.category), datasets: [{ data: view.categories.map((item) => item.amount), backgroundColor: CHART_COLORS, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
      return;
    }
    const labels = view.trend.labels.map((label) => (view.trend.unit === 'day' ? formatDate(label) : label));
    mountChart(canvas, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'Income', data: view.trend.income, backgroundColor: CHART_COLORS[0], borderRadius: 6 },
          { label: 'Expenses', data: view.trend.expense, backgroundColor: CHART_COLORS[3], borderRadius: 6 },
        ],
      },
      options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
    });
  }
}

function cell(value: unknown): string {
  const text = String(value ?? '');
  return /[",\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text;
}

function csv(rows: Tx[]): string {
  const lines = ['Date,Name,Category,Type,Amount', ...rows.map((row) => [row.date, row.name, row.category, row.type, row.amount].map(cell).join(','))];
  return lines.join('\r\n');
}

function summaryText(from: string, to: string, rows: Tx[], title: string): string {
  const view = reportAnalysis(rows, from, to);
  return [
    'MicroVault financial report',
    '===========================',
    'Report: ' + title,
    'Period: ' + formatDate(from) + ' to ' + formatDate(to),
    '',
    'Income: ' + formatINR(view.income),
    'Expenses: ' + formatINR(view.expense),
    'Net saved: ' + formatINR(view.net),
    'Savings rate: ' + (view.savingsRate === null ? 'No income recorded' : view.savingsRate + '%'),
    'Transactions: ' + view.count,
    '',
    'Spending by category',
    '--------------------',
    ...(view.categories.length ? view.categories.map((item) => `${item.category}: ${formatINR(item.amount)} (${item.percent}%)`) : ['No expenses in this period']),
    '',
    'Transactions',
    '------------',
    ...(rows.length ? rows.map((row) => `${row.date}  ${row.name}  [${row.category}, ${row.type}]  ${formatINR(row.amount)}`) : ['No transactions in this period']),
  ].join('\r\n');
}
