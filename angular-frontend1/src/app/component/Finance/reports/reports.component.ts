import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { Validators, formatDate, formatINR, monthStartISO, queryRows, todayISO } from '../../../shared/format';
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
  private readonly dialog = inject(DialogService);
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  fromDate = monthStartISO();
  toDate = todayISO();
  type = 'Summary';
  readonly preview = signal<Report | null>(null);
  readonly search = signal('');
  readonly filterType = signal('');
  readonly pageNo = signal(1);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly page = computed(() => queryRows(this.vault.reports(), this.search(), ['type', 'fromDate', 'toDate'], { type: this.filterType() }, this.pageNo(), 6));

  async generate(): Promise<void> {
    const errors = {
      from: Validators.dateNotFuture(this.fromDate, 'From date'),
      to: Validators.dateNotFuture(this.toDate, 'To date') || (this.toDate < this.fromDate ? 'To date must be on or after the from date' : ''),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      this.preview.set(await this.vault.generateReport({ fromDate: this.fromDate, toDate: this.toDate, type: this.type }));
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not generate report');
    } finally { this.busy.set(false); }
  }
  download(report: Report, format: 'txt' | 'csv'): void {
    const txs = this.vault.transactions().filter((row) => row.date >= report.fromDate && row.date <= report.toDate);
    const lines = [
      'MicroVault Financial Report',
      '===========================',
      'Type: ' + report.type,
      'Generated: ' + formatDate(report.createdAt || todayISO()),
      'Period: ' + formatDate(report.fromDate) + ' to ' + formatDate(report.toDate),
      'Income: ' + formatINR(report.income),
      'Expense: ' + formatINR(report.expense),
      'Net: ' + formatINR(report.net),
      'Transactions counted: ' + (report.txCount || txs.length),
      '',
      'Detailed transactions',
      '--------------------',
      'Date,Name,Category,Type,Amount',
      ...(txs.length ? txs.map((row: Tx) => [row.date, '"' + (row.name || '').replace(/"/g, "'") + '"', row.category, row.type, row.amount].join(',')) : ['(No transactions in this period)']),
      '',
      'Prepared by MicroVault — business finance workspace',
    ];
    const blob = new Blob([lines.join('\r\n')], { type: format === 'csv' ? 'text/csv;charset=utf-8' : 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `MicroVault_${(report.type || 'Report').replace(/\s+/g, '_')}_${report.fromDate}_to_${report.toDate}.${format}`;
    link.click();
    URL.revokeObjectURL(url);
  }
  async remove(row: Report): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this report?')) await this.vault.deleteReport(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every report?')) await this.vault.clearReports();
  }
}
