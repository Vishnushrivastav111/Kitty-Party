import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { destroyChart, mountChart } from '../../shared/charts';
import { stepGraph } from '../../shared/finance-rules';
import { CHART_COLORS, formatDate, queryRows } from '../../shared/format';
import { AdminInsights } from '../../model/models';
import { AdminService } from '../../service/admin.service';
import { GraphPagerComponent } from '../../shared/graph-pager.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent, GraphPagerComponent],
  templateUrl: './admin-overview.component.html',
  styleUrl: './admin-overview.component.scss',
})
export class AdminOverviewComponent {
  private readonly vault = inject(AdminService);
  private readonly stage = viewChild<ElementRef<HTMLCanvasElement>>('stage');
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly pageNo = signal(1);
  readonly graphIndex = signal(0);
  readonly pages = [
    { id: 'status', title: 'Member status', empty: 'Member status counts are not available.' },
    { id: 'setup', title: 'Finance setup', empty: 'Setup counts are not available.' },
    { id: 'joins', title: 'Member joins by month', empty: 'No join history is available for this graph.' },
    { id: 'activity', title: 'Feedback and news', empty: 'Feedback and news counts are not available.' },
  ];
  readonly insights = computed(() => this.vault.insights());
  readonly activeGraph = computed(() => this.pages[Math.min(this.graphIndex(), this.pages.length - 1)]);
  readonly graphEmpty = computed(() => this.emptyFor(this.activeGraph().id));
  readonly page = computed(() => queryRows(this.vault.members(), this.search(), ['fullName', 'email', 'phone'], { status: this.status() }, this.pageNo(), 6));

  constructor() {
    effect(() => this.paint());
  }

  showGraph(index: number): void {
    destroyChart(this.stage()?.nativeElement);
    this.graphIndex.set(stepGraph(index, this.pages.length, 0));
  }

  private emptyFor(id: string): string {
    const data = this.insights();
    if (id === 'status' && !data.statusLabels.length) return this.pages[0].empty;
    if (id === 'setup' && !data.setupLabels.length) return this.pages[1].empty;
    if (id === 'joins' && !data.joinLabels.length) return this.pages[2].empty;
    return '';
  }

  private paint(): void {
    const graph = this.activeGraph();
    const canvas = this.stage()?.nativeElement;
    const data = this.insights();
    if (this.graphEmpty() || !canvas) {
      destroyChart(canvas);
      return;
    }
    const config = this.configFor(graph.id, data);
    if (!config) {
      destroyChart(canvas);
      return;
    }
    mountChart(canvas, config);
  }

  private configFor(id: string, data: AdminInsights): ChartConfiguration | null {
    if (id === 'status') {
      return { type: 'pie', data: { labels: data.statusLabels, datasets: [{ data: data.statusValues, backgroundColor: [CHART_COLORS[5], CHART_COLORS[3]], borderWidth: 0 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } };
    }
    if (id === 'setup') {
      return { type: 'bar', data: { labels: data.setupLabels, datasets: [{ data: data.setupValues, backgroundColor: [CHART_COLORS[0], CHART_COLORS[2]], borderRadius: 8 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } };
    }
    if (id === 'joins') {
      return { type: 'line', data: { labels: data.joinLabels, datasets: [{ label: 'Joins', data: data.joinValues, borderColor: CHART_COLORS[1], tension: 0.35, fill: false }] }, options: { responsive: true, maintainAspectRatio: false } };
    }
    return { type: 'bar', data: { labels: ['Feedback', 'Open', 'News', 'Published'], datasets: [{ data: [data.feedbackTotal, data.feedbackOpen, data.newsTotal, data.publishedNews], backgroundColor: CHART_COLORS.slice(0, 4), borderRadius: 8 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } };
  }
}
