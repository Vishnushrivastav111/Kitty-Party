import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../core/charts';
import { DialogService } from '../core/dialog.service';
import { CHART_COLORS, GOAL_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../core/format';
import { Goal } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Goals</h1><p>Plan targets, track progress, and update saved amounts</p></div>
    <div class="charts-grid">
      <div class="chart-card"><h3><i class="fas fa-chart-bar"></i> Saved vs target</h3><div class="chart-wrap"><canvas #goalBar></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-pie"></i> Status mix</h3><div class="chart-wrap"><canvas #goalPie></canvas></div></div>
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Goal list</h2>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Add goal</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search goals..." [ngModel]="search()" (ngModelChange)="setSearch($event)" />
        <select [ngModel]="status()" (ngModelChange)="setFilter('status', $event)">
          <option value="">All statuses</option><option value="active">Active</option><option value="completed">Completed</option><option value="paused">Paused</option>
        </select>
        <select [ngModel]="category()" (ngModelChange)="setFilter('category', $event)">
          <option value="">All categories</option>
          @for (item of categories; track item) { <option [value]="item">{{ item }}</option> }
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Title</th><th>Category</th><th>Target</th><th>Saved</th><th>Progress</th><th>Deadline</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="8" class="empty-state">No goals yet</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ row.title }}</td><td>{{ row.category }}</td><td>{{ money(row.target) }}</td><td>{{ money(row.saved) }}</td>
                <td>{{ pct(row) }}%</td><td>{{ date(row.deadline) }}</td>
                <td><span class="badge" [class.badge-success]="row.status === 'completed'" [class.badge-warning]="row.status === 'paused'" [class.badge-info]="row.status === 'active'">{{ row.status }}</span></td>
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
        <h3>{{ form.id ? 'Edit goal' : 'Add goal' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Title</label><input name="title" [(ngModel)]="form.title" [class.input-error]="errors()['title']" /><span class="field-error" [style.display]="errors()['title'] ? 'block' : 'none'">{{ errors()['title'] }}</span></div>
            <div class="form-group"><label>Category</label>
              <select name="category" [(ngModel)]="form.category"><option value="">Select</option>@for (item of categories; track item) { <option [value]="item">{{ item }}</option> }</select>
              <span class="field-error" [style.display]="errors()['category'] ? 'block' : 'none'">{{ errors()['category'] }}</span>
            </div>
            <div class="form-group"><label>Target (₹)</label><input type="number" name="target" min="1" [(ngModel)]="form.target" /><span class="field-error" [style.display]="errors()['target'] ? 'block' : 'none'">{{ errors()['target'] }}</span></div>
            <div class="form-group"><label>Saved (₹)</label><input type="number" name="saved" min="0" [(ngModel)]="form.saved" /><span class="field-error" [style.display]="errors()['saved'] ? 'block' : 'none'">{{ errors()['saved'] }}</span></div>
            <div class="form-group"><label>Deadline</label><input type="date" name="deadline" [(ngModel)]="form.deadline" /><span class="field-error" [style.display]="errors()['deadline'] ? 'block' : 'none'">{{ errors()['deadline'] }}</span></div>
            <div class="form-group"><label>Status</label><select name="status" [(ngModel)]="form.status"><option value="active">Active</option><option value="paused">Paused</option><option value="completed">Completed</option></select></div>
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
export class GoalsComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  private readonly bar = viewChild<ElementRef<HTMLCanvasElement>>('goalBar');
  private readonly pie = viewChild<ElementRef<HTMLCanvasElement>>('goalPie');
  readonly categories = GOAL_CATEGORIES;
  readonly money = formatINR;
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form = blank();
  readonly page = computed(() => queryRows(this.vault.goals(), this.search(), ['title', 'category', 'status'], { status: this.status(), category: this.category() }, this.pageNo(), 6));

  constructor() {
    effect(() => {
      const goals = this.vault.goals();
      const bar = this.bar()?.nativeElement;
      const pie = this.pie()?.nativeElement;
      if (!bar || !pie) return;
      const labels = goals.map((goal) => goal.title);
      mountChart(bar, {
        type: 'bar',
        data: {
          labels: labels.length ? labels : ['No goals'],
          datasets: [
            { label: 'Saved', data: goals.length ? goals.map((goal) => num(goal.saved)) : [0], backgroundColor: CHART_COLORS[0], borderRadius: 6 },
            { label: 'Target', data: goals.length ? goals.map((goal) => num(goal.target)) : [0], backgroundColor: CHART_COLORS[1], borderRadius: 6 },
          ],
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } },
      });
      const mix: Record<string, number> = {};
      goals.forEach((goal) => { mix[goal.status || 'active'] = (mix[goal.status || 'active'] || 0) + 1; });
      mountChart(pie, {
        type: 'pie',
        data: { labels: Object.keys(mix).length ? Object.keys(mix) : ['No data'], datasets: [{ data: Object.keys(mix).length ? Object.values(mix) : [1], backgroundColor: CHART_COLORS, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      });
    });
  }

  setSearch(value: string): void { this.search.set(value); this.pageNo.set(1); }
  setFilter(key: 'status' | 'category', value: string): void {
    if (key === 'status') this.status.set(value); else this.category.set(value);
    this.pageNo.set(1);
  }
  pct(row: Goal): number { return Math.min(100, Math.round((num(row.saved) / (num(row.target) || 1)) * 100)); }
  open(row: Goal | null): void {
    this.form = row ? { ...row, target: num(row.target), saved: num(row.saved), deadline: (row.deadline || '').slice(0, 10) } : blank();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = {
      title: Validators.required(this.form.title, 'Title'),
      category: Validators.required(this.form.category, 'Category'),
      target: Number(this.form.target) > 0 ? '' : 'Target must be greater than zero',
      saved: Validators.amount(this.form.saved, 'Saved amount'),
      deadline: this.form.deadline ? '' : 'Deadline is required',
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { title: this.form.title.trim(), category: this.form.category, target: Number(this.form.target), saved: Number(this.form.saved), deadline: this.form.deadline, status: this.form.status };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateGoal(this.form.id, body); else await this.vault.addGoal(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save goal');
    } finally { this.busy.set(false); }
  }
  async remove(row: Goal): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this goal?')) await this.vault.deleteGoal(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every goal? This action cannot be undone.')) await this.vault.clearGoals();
  }
}

function blank(): Goal {
  return { id: '', title: '', category: '', target: 0, saved: 0, deadline: todayISO(), status: 'active' };
}
