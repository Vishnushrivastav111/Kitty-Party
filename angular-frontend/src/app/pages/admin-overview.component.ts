import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { mountChart } from '../core/charts';
import { CHART_COLORS, formatDate, queryRows } from '../core/format';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent],
  template: `
    <div class="page-header"><h1>Admin dashboard</h1><p>User insights, activity trends, and platform health</p></div>
    <div class="cards">
      <div class="card"><div class="card-icon navy"><i class="fas fa-users"></i></div><div><h3>Members</h3><h2>{{ insights().totalMembers }}</h2><p>{{ insights().activeMembers }} active</p></div></div>
      <div class="card"><div class="card-icon teal"><i class="fas fa-user-shield"></i></div><div><h3>Admins</h3><h2>{{ insights().adminCount }}</h2></div></div>
      <div class="card"><div class="card-icon green"><i class="fas fa-sliders"></i></div><div><h3>Setup done</h3><h2>{{ insights().setupDone }}</h2><p>{{ insights().setupPending }} pending</p></div></div>
      <div class="card"><div class="card-icon orange"><i class="fas fa-comments"></i></div><div><h3>Open feedback</h3><h2>{{ insights().feedbackOpen }}</h2><p>{{ insights().feedbackTotal }} total</p></div></div>
    </div>
    <div class="charts-grid">
      <div class="chart-card"><h3><i class="fas fa-chart-pie"></i> Member status</h3><div class="chart-wrap"><canvas #statusPie></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-column"></i> Finance setup</h3><div class="chart-wrap"><canvas #setupBar></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-line"></i> Member joins by month</h3><div class="chart-wrap"><canvas #joinsLine></canvas></div></div>
      <div class="chart-card"><h3><i class="fas fa-chart-simple"></i> Feedback &amp; news</h3><div class="chart-wrap"><canvas #activityBar></canvas></div></div>
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Member insights</h2>
        <a class="btn btn-primary btn-sm" routerLink="/admin/users"><i class="fas fa-users"></i> Manage users</a>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search members..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="status()" (ngModelChange)="status.set($event); pageNo.set(1)">
          <option value="">All statuses</option><option value="active">Active</option><option value="inactive">Inactive</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Joined</th><th>Setup</th><th>Status</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="6" class="empty-state">No members yet</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ row.fullName }}</td><td>{{ row.email }}</td><td>{{ row.phone || '—' }}</td><td>{{ date(row.createdAt) }}</td>
                <td><span class="badge" [class.badge-success]="row.setupComplete" [class.badge-warning]="!row.setupComplete">{{ row.setupComplete ? 'Done' : 'Pending' }}</span></td>
                <td><span class="badge" [class.badge-success]="row.status === 'active'" [class.badge-neutral]="row.status !== 'active'">{{ row.status || 'active' }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
  `,
})
export class AdminOverviewComponent {
  private readonly vault = inject(VaultService);
  private readonly statusPie = viewChild<ElementRef<HTMLCanvasElement>>('statusPie');
  private readonly setupBar = viewChild<ElementRef<HTMLCanvasElement>>('setupBar');
  private readonly joinsLine = viewChild<ElementRef<HTMLCanvasElement>>('joinsLine');
  private readonly activityBar = viewChild<ElementRef<HTMLCanvasElement>>('activityBar');
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly insights = computed(() => this.vault.insights());
  readonly page = computed(() => queryRows(this.vault.members(), this.search(), ['fullName', 'email', 'phone'], { status: this.status() }, this.pageNo(), 6));

  constructor() {
    effect(() => {
      const data = this.insights();
      const pie = this.statusPie()?.nativeElement;
      const setup = this.setupBar()?.nativeElement;
      const joins = this.joinsLine()?.nativeElement;
      const activity = this.activityBar()?.nativeElement;
      if (!pie || !setup || !joins || !activity) return;
      mountChart(pie, { type: 'pie', data: { labels: data.statusLabels, datasets: [{ data: data.statusValues.length ? data.statusValues : [0, 0], backgroundColor: [CHART_COLORS[5], CHART_COLORS[3]], borderWidth: 0 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } });
      mountChart(setup, { type: 'bar', data: { labels: data.setupLabels, datasets: [{ data: data.setupValues.length ? data.setupValues : [0, 0], backgroundColor: [CHART_COLORS[0], CHART_COLORS[2]], borderRadius: 8 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } });
      mountChart(joins, { type: 'line', data: { labels: data.joinLabels.length ? data.joinLabels : ['—'], datasets: [{ label: 'Joins', data: data.joinValues.length ? data.joinValues : [0], borderColor: CHART_COLORS[1], tension: 0.35, fill: false }] }, options: { responsive: true, maintainAspectRatio: false } });
      mountChart(activity, { type: 'bar', data: { labels: ['Feedback', 'Open', 'News', 'Published'], datasets: [{ data: [data.feedbackTotal, data.feedbackOpen, data.newsTotal, data.publishedNews], backgroundColor: CHART_COLORS.slice(0, 4), borderRadius: 8 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } });
    });
  }
}
