import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { parseApiError } from '../../../shared/api-error';
import { assignErrors } from '../../../shared/field-state';
import { reportDateErrors, reportRequestBody, totalsFromTransactions, transactionsMatching } from '../../../shared/finance-rules';
import { TX_CATEGORIES, formatDate, formatINR, monthStartISO, queryRows, todayISO } from '../../../shared/format';
import { Report, Tx } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent {
  private readonly vault = inject(FinanceService);
  readonly categories = TX_CATEGORIES;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  fromDate = monthStartISO();
  toDate = todayISO();
  reportName = 'Summary';
  txType = 'all';
  category = 'all';
  readonly preview = signal<Report | null>(null);
  readonly applied = signal<{ from: string; to: string; type: string; category: string } | null>(null);
  readonly matched = signal<Tx[]>([]);
  readonly matchedTotals = computed(() => totalsFromTransactions(this.matched()));
  readonly search = signal('');
  readonly filterType = signal('');
  readonly pageNo = signal(1);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly page = computed(() => queryRows(this.vault.reports(), this.search(), ['type', 'fromDate', 'toDate'], { type: this.filterType() }, this.pageNo(), 6));

  touchDates(): void {
    this.errors.update((current) => assignErrors(current, reportDateErrors(this.fromDate, this.toDate, this.today)));
  }

  async generate(): Promise<void> {
    const dateErrors = reportDateErrors(this.fromDate, this.toDate, this.today);
    this.errors.set(dateErrors);
    this.formError.set('');
    if (dateErrors.from || dateErrors.to) return;
    const criteria = { from: this.fromDate, to: this.toDate, type: this.txType, category: this.category };
    const body = reportRequestBody(criteria.from, criteria.to, this.reportName.trim() || 'Summary');
    this.busy.set(true);
    try {
      const report = await this.vault.generateReport(body);
      this.preview.set(report);
      this.applied.set(criteria);
      this.matched.set(transactionsMatching(this.vault.transactions(), criteria.from, criteria.to, criteria.type, criteria.category));
    } catch (error) {
      this.preview.set(null);
      const parsed = parseApiError(error);
      this.formError.set(parsed.message);
      this.errors.update((current) => assignErrors(current, parsed.fieldErrors));
    } finally {
      this.busy.set(false);
    }
  }

  showSaved(row: Report): void {
    const criteria = { from: row.fromDate, to: row.toDate, type: 'all', category: 'all' };
    this.preview.set(row);
    this.applied.set(criteria);
    this.matched.set(transactionsMatching(this.vault.transactions(), criteria.from, criteria.to, criteria.type, criteria.category));
  }

  download(report: Report, format: 'txt' | 'csv'): void {
    const txs = transactionsMatching(this.vault.transactions(), report.fromDate, report.toDate, 'all', 'all');
    const lines = [
      'MicroVault Financial Report',
      '===========================',
      'Type: ' + report.type,
      'Generated: ' + formatDate(report.createdAt || todayISO()),
      'Period: ' + formatDate(report.fromDate) + ' to ' + formatDate(report.toDate),
      'Income: ' + formatINR(report.income),
      'Expense: ' + formatINR(report.expense),
      'Net: ' + formatINR(report.net),
      'Transactions counted: ' + report.txCount,
      '',
      'Transactions loaded for this date range',
      '---------------------------------------',
      'Date,Name,Category,Type,Amount',
      ...(txs.length ? txs.map((row: Tx) => [row.date, '"' + (row.name || '').replace(/"/g, "'") + '"', row.category, row.type, row.amount].join(',')) : ['(No loaded transactions in this period)']),
    ];
    const blob = new Blob([lines.join('\r\n')], { type: format === 'csv' ? 'text/csv;charset=utf-8' : 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `MicroVault_${(report.type || 'Report').replace(/\s+/g, '_')}_${report.fromDate}_to_${report.toDate}.${format}`;
    link.click();
    URL.revokeObjectURL(url);
  }
}
