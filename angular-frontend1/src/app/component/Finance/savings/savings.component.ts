import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { destroyChart, mountChart } from '../../../shared/charts';
import { savingsTrend, stepGraph } from '../../../shared/finance-rules';
import { GraphPagerComponent } from '../../../shared/graph-pager.component';
import { parseApiError } from '../../../shared/api-error';
import { DialogService } from '../../../shared/dialog.service';
import { assignError } from '../../../shared/field-state';
import { CHART_COLORS, SAVINGS_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { Saving } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-savings',
  standalone: true,
  imports: [FormsModule, PagerComponent, GraphPagerComponent],
  templateUrl: './savings.component.html',
  styleUrl: './savings.component.scss',
})
export class SavingsComponent {
  readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');
  readonly graphIndex = signal(0);
  readonly categories = SAVINGS_CATEGORIES;
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  readonly search = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: Saving = blank();
  /** The single savings picture calculated by the finance service. */
  readonly overview = this.vault.savingsOverview;
  readonly page = computed(() => queryRows(this.vault.savings(), this.search(), ['title', 'note', 'category'], { category: this.category() }, this.pageNo(), 6));

  constructor() {
    effect(() => this.paint());
  }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, 2, 0));
  }

  private paint(): void {
    const canvas = this.stage()?.nativeElement;
    const rows = this.vault.savings();
    if (!rows.length || !canvas) {
      destroyChart(canvas);
      return;
    }
    if (this.graphIndex() === 0) {
      const series = savingsTrend([...rows].reverse());
      if (!series) return;
      mountChart(canvas, {
        type: 'line',
        data: { labels: series.labels.map((label) => formatDate(label)), datasets: [{ label: 'Recorded entries', data: series.data, borderColor: CHART_COLORS[0], backgroundColor: 'rgba(13,148,136,0.15)', fill: true, tension: 0.35 }] },
        options: { responsive: true, maintainAspectRatio: false },
      });
      return;
    }
    const mix: Record<string, number> = {};
    rows.forEach((row) => { mix[row.category || 'Other'] = (mix[row.category || 'Other'] || 0) + num(row.amount); });
    mountChart(canvas, {
      type: 'pie',
      data: { labels: Object.keys(mix), datasets: [{ data: Object.values(mix), backgroundColor: CHART_COLORS, borderWidth: 0 }] },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
    });
  }

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  open(row: Saving | null): void {
    this.form = row ? { ...row, amount: num(row.amount), date: (row.date || '').slice(0, 10) } : blank();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = {
      title: Validators.required(this.form.title, 'Title'),
      category: Validators.required(this.form.category, 'Category'),
      amount: Number(this.form.amount) > 0 ? '' : 'Amount must be greater than zero',
      date: Validators.dateNotFuture(this.form.date),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { title: this.form.title.trim(), category: this.form.category, amount: Number(this.form.amount), date: this.form.date, note: this.form.note || '', goalId: this.form.goalId || null };
    this.busy.set(true);
    try {
      const warning = this.form.id ? await this.vault.updateSaving(this.form.id, body) : await this.vault.addSaving(body);
      this.editing.set(false);
      if (warning) await this.dialog.notice(warning, 'Saved, refresh failed');
    } catch (error) {
      const parsed = parseApiError(error);
      this.errors.set({ ...errors, ...parsed.fieldErrors, form: parsed.message });
    } finally { this.busy.set(false); }
  }
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  titleOk(value: string): string { return Validators.required(value, 'Title'); }
  amountOk(value: unknown): string { return Number(value) > 0 ? '' : 'Amount must be greater than zero'; }
  dateOk(value: string): string { return Validators.dateNotFuture(value); }
  async remove(row: Saving): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this savings entry?')) await this.vault.deleteSaving(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every savings entry? This action cannot be undone.')) await this.vault.clearSavings();
  }
}

function blank(): Saving {
  return { id: '', title: '', category: 'Deposit', amount: 0, date: todayISO(), note: '', goalId: '' };
}
