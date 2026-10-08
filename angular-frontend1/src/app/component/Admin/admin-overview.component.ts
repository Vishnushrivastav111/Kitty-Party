import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { mountChart } from '../../shared/charts';
import { CHART_COLORS, formatDate, queryRows } from '../../shared/format';
import { AdminService } from '../../service/admin.service';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [FormsModule, RouterLink, PagerComponent],
  templateUrl: './admin-overview.component.html',
  styleUrl: './admin-overview.component.scss',
})
export class AdminOverviewComponent {
  private readonly vault = inject(AdminService);
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
