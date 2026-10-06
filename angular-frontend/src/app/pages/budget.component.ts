import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../core/charts';
import { DialogService } from '../core/dialog.service';
import { BUDGET_CATEGORIES, CHART_COLORS, Validators, formatINR, num, queryRows } from '../core/format';
import { Budget } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Budget</h1><p>Set category limits and monitor spending</p></div>
    <div class="cards">
      <div class="card"><div class="card-icon navy"><i class="fas fa-wallet"></i></div><div><h3>Total limits</h3><h2>{{ money(totals().limit) }}</h2></div></div>
      <div class="card"><div class="card-icon orange"><i class="fas fa-receipt"></i></div><div><h3>Total spent</h3><h2>{{ money(totals().spent) }}</h2></div></div>
      <div class="card"><div class="card-icon" [class.orange]="totals().spent > totals().limit" [class.green]="totals().spent <= totals().limit"><i class="fas fa-scale-balanced"></i></div><div><h3>Remaining</h3><h2>{{ money(totals().limit - totals().spent) }}</h2></div></div>
    </div>
    <div class="charts-grid">
      <div class="chart-card"><h3><i class="fas fa-chart-bar"></i> Limit vs spent</h3><div class="chart-wrap"><canvas #budgetBar></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-pie"></i> Budget allocation</h3><div class="chart-wrap"><canvas #budgetPie></canvas></div></div>
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Budget categories</h2>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Add budget</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search category..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="status()" (ngModelChange)="status.set($event); pageNo.set(1)">
          <option value="">All</option><option value="ok">Within limit</option><option value="warn">Near limit</option><option value="over">Over budget</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Category</th><th>Limit</th><th>Spent</th><th>Remaining</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="6" class="empty-state">No budgets set</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ row.category }}</td><td>{{ money(row.limit) }}</td><td>{{ money(row.spent) }}</td><td>{{ money(row.remaining) }}</td>
                <td><span class="badge" [class.badge-success]="row.status === 'ok'" [class.badge-warning]="row.status === 'warn'" [class.badge-danger]="row.status === 'over'">{{ label(row.status) }}</span></td>
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
        <h3>{{ form.id ? 'Edit budget' : 'Add budget' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Category</label>
              <select name="category" [(ngModel)]="form.category"><option value="">Select</option>@for (item of categories; track item) { <option [value]="item">{{ item }}</option> }</select>
              <span class="field-error" [style.display]="errors()['category'] ? 'block' : 'none'">{{ errors()['category'] }}</span>
            </div>
            <div class="form-group"><label>Monthly limit (₹)</label><input type="number" name="limit" min="1" [(ngModel)]="form.limit" /><span class="field-error" [style.display]="errors()['limit'] ? 'block' : 'none'">{{ errors()['limit'] }}</span></div>
            <div class="form-group"><label>Note</label><input name="note" [(ngModel)]="form.note" /></div>
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
export class BudgetComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  private readonly bar = viewChild<ElementRef<HTMLCanvasElement>>('budgetBar');
  private readonly pie = viewChild<ElementRef<HTMLCanvasElement>>('budgetPie');
  readonly categories = BUDGET_CATEGORIES;
  readonly money = formatINR;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: Budget = { id: '', category: '', limit: 0, note: '' };
  readonly totals = computed(() => this.vault.budgets().reduce((sum, row) => ({ limit: sum.limit + num(row.limit), spent: sum.spent + num(row.spent) }), { limit: 0, spent: 0 }));
  readonly page = computed(() => queryRows(this.vault.budgets(), this.search(), ['category', 'note'], { status: this.status() }, this.pageNo(), 6));

  constructor() {
    effect(() => {
      const rows = this.vault.budgets();
      const bar = this.bar()?.nativeElement;
      const pie = this.pie()?.nativeElement;
      if (!bar || !pie) return;
      const labels = rows.map((row) => row.category);
      mountChart(bar, {
        type: 'bar',
        data: {
          labels: labels.length ? labels : ['No budgets'],
          datasets: [
            { label: 'Limit', data: rows.length ? rows.map((row) => num(row.limit)) : [0], backgroundColor: CHART_COLORS[1], borderRadius: 6 },
            { label: 'Spent', data: rows.length ? rows.map((row) => num(row.spent)) : [0], backgroundColor: CHART_COLORS[2], borderRadius: 6 },
          ],
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
      });
      mountChart(pie, {
        type: 'pie',
        data: { labels: labels.length ? labels : ['No data'], datasets: [{ data: rows.length ? rows.map((row) => num(row.limit)) : [1], backgroundColor: CHART_COLORS, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
    });
  }

  label(status?: string): string {
    if (status === 'over') return 'Over budget';
    if (status === 'warn') return 'Near limit';
    return 'Within limit';
  }
  open(row: Budget | null): void {
    this.form = row ? { ...row, limit: num(row.limit), note: row.note || '' } : { id: '', category: '', limit: 0, note: '' };
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = { category: Validators.required(this.form.category, 'Category'), limit: Number(this.form.limit) > 0 ? '' : 'Limit must be greater than zero' };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { category: this.form.category, limit: Number(this.form.limit), note: this.form.note || '' };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateBudget(this.form.id, body); else await this.vault.addBudget(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save budget');
    } finally { this.busy.set(false); }
  }
  async remove(row: Budget): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this budget?')) await this.vault.deleteBudget(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every budget? This action cannot be undone.')) await this.vault.clearBudgets();
  }
}
