import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mountChart } from '../../../shared/charts';
import { DialogService } from '../../../shared/dialog.service';
import { CHART_COLORS, GOAL_CATEGORIES, Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { Goal } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent {
  private readonly vault = inject(FinanceService);
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
