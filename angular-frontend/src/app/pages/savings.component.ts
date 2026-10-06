import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../core/charts';
import { DialogService } from '../core/dialog.service';
import { CHART_COLORS, SAVINGS_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../core/format';
import { Saving } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-savings',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Savings</h1><p>Record deposits and withdrawals toward your vault</p></div>
    <div class="cards">
      <div class="card"><div class="card-icon green"><i class="fas fa-piggy-bank"></i></div><div><h3>Vault total</h3><h2>{{ money(total()) }}</h2></div></div>
      <div class="card"><div class="card-icon teal"><i class="fas fa-list"></i></div><div><h3>Entries</h3><h2>{{ vault.savings().length }}</h2></div></div>
    </div>
    <div class="charts-grid">
      <div class="chart-card"><h3><i class="fas fa-chart-line"></i> Balance growth</h3><div class="chart-wrap"><canvas #savLine></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-pie"></i> By category</h3><div class="chart-wrap"><canvas #savPie></canvas></div></div>
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Savings history</h2>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Add entry</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search title or note..." [ngModel]="search()" (ngModelChange)="setSearch($event)" />
        <select [ngModel]="category()" (ngModelChange)="category.set($event); pageNo.set(1)">
          <option value="">All categories</option>
          @for (item of categories; track item) { <option [value]="item">{{ item }}</option> }
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Amount</th><th>Note</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="6" class="empty-state">No savings entries</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ date(row.date) }}</td><td>{{ row.title }}</td><td>{{ row.category }}</td><td>{{ money(row.amount) }}</td><td>{{ row.note || '—' }}</td>
                <td>
                  <button class="btn btn-outline btn-sm" type="button" (click)="open(row)"><i class="fas fa-pen"></i></button>
                  <button class="btn btn-danger btn-sm" type="button" (click)="remove(row)"><i class="fas fa-trash"></i></button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
    <div class="modal-backdrop" [class.open]="editing()">
      <div class="modal">
        <h3>{{ form.id ? 'Edit entry' : 'Add entry' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Title</label><input name="title" [(ngModel)]="form.title" /><span class="field-error" [style.display]="errors()['title'] ? 'block' : 'none'">{{ errors()['title'] }}</span></div>
            <div class="form-group"><label>Category</label>
              <select name="category" [(ngModel)]="form.category"><option value="">Select</option>@for (item of categories; track item) { <option [value]="item">{{ item }}</option> }</select>
              <span class="field-error" [style.display]="errors()['category'] ? 'block' : 'none'">{{ errors()['category'] }}</span>
            </div>
            <div class="form-group"><label>Amount (₹)</label><input type="number" name="amount" min="1" [(ngModel)]="form.amount" /><span class="field-error" [style.display]="errors()['amount'] ? 'block' : 'none'">{{ errors()['amount'] }}</span></div>
            <div class="form-group"><label>Date</label><input type="date" name="date" [(ngModel)]="form.date" [max]="today" /><span class="field-error" [style.display]="errors()['date'] ? 'block' : 'none'">{{ errors()['date'] }}</span></div>
            <div class="form-group" style="grid-column:1/-1"><label>Note</label><input name="note" [(ngModel)]="form.note" /></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-outline" (click)="editing.set(false)">Cancel</button>
            <button class="btn btn-primary" [disabled]="busy()">Save</button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class SavingsComponent {
  readonly vault = inject(VaultService);
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
