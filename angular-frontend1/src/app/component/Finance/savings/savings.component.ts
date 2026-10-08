import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../../../shared/charts';
import { DialogService } from '../../../shared/dialog.service';
import { CHART_COLORS, SAVINGS_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { Saving } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-savings',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './savings.component.html',
  styleUrl: './savings.component.scss',
})
export class SavingsComponent {
  readonly vault = inject(FinanceService);
  private readonly dialog = inject(DialogService);
  private readonly line = viewChild<ElementRef<HTMLCanvasElement>>('savLine');
  private readonly pie = viewChild<ElementRef<HTMLCanvasElement>>('savPie');
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
  readonly total = computed(() => this.vault.savings().reduce((sum, row) => sum + num(row.amount), 0));
  readonly page = computed(() => queryRows(this.vault.savings(), this.search(), ['title', 'note', 'category'], { category: this.category() }, this.pageNo(), 6));

  constructor() {
    effect(() => {
      const rows = [...this.vault.savings()].reverse();
      const line = this.line()?.nativeElement;
      const pie = this.pie()?.nativeElement;
      if (!line || !pie) return;
      let running = 0;
      const labels: string[] = [];
      const data: number[] = [];
      rows.forEach((row) => { running += num(row.amount); labels.push(formatDate(row.date)); data.push(running); });
      mountChart(line, {
        type: 'line',
        data: { labels: labels.length ? labels : ['Start'], datasets: [{ label: 'Balance', data: data.length ? data : [0], borderColor: CHART_COLORS[0], backgroundColor: 'rgba(13,148,136,0.15)', fill: true, tension: 0.35 }] },
        options: { responsive: true, maintainAspectRatio: false },
      });
      const mix: Record<string, number> = {};
      this.vault.savings().forEach((row) => { mix[row.category || 'Other'] = (mix[row.category || 'Other'] || 0) + num(row.amount); });
      mountChart(pie, {
        type: 'pie',
        data: { labels: Object.keys(mix).length ? Object.keys(mix) : ['No data'], datasets: [{ data: Object.keys(mix).length ? Object.values(mix) : [1], backgroundColor: CHART_COLORS, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
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
    const body = { title: this.form.title.trim(), category: this.form.category, amount: Number(this.form.amount), date: this.form.date, note: this.form.note || '' };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateSaving(this.form.id, body); else await this.vault.addSaving(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save entry');
    } finally { this.busy.set(false); }
  }
  async remove(row: Saving): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this savings entry?')) await this.vault.deleteSaving(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every savings entry? This action cannot be undone.')) await this.vault.clearSavings();
  }
}

function blank(): Saving {
  return { id: '', title: '', category: 'Deposit', amount: 0, date: todayISO(), note: '' };
}
