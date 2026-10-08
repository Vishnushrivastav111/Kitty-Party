import { Injectable, inject, signal } from '@angular/core';
import { asArray, num } from '../shared/format';
import {
  AdminInsights,
  AffordCheck,
  Budget,
  DashboardPayload,
  DashStats,
  Feedback,
  FinanceProfile,
  Goal,
  NewsItem,
  Notice,
  Report,
  Saving,
  Tx,
  VaultUser,
} from '../model/models';
import { ApiError, ApiService } from './api.service';
import { AuthService } from './auth.service';

const EMPTY_INSIGHTS: AdminInsights = {
  totalMembers: 0, activeMembers: 0, inactiveMembers: 0, adminCount: 0,
  setupDone: 0, setupPending: 0, feedbackTotal: 0, feedbackOpen: 0,
  newsTotal: 0, publishedNews: 0, joinLabels: [], joinValues: [],
  statusLabels: ['Active', 'Inactive'], statusValues: [0, 0],
  setupLabels: ['Setup done', 'Pending'], setupValues: [0, 0], recentMembers: [],
};

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  readonly loaded = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly finance = signal<FinanceProfile | null>(null);
  readonly setupSkipped = signal(false);
  readonly stats = signal<DashStats | null>(null);
  readonly transactions = signal<Tx[]>([]);
  readonly goals = signal<Goal[]>([]);
  readonly savings = signal<Saving[]>([]);
  readonly budgets = signal<Budget[]>([]);
  readonly reports = signal<Report[]>([]);
  readonly affordChecks = signal<AffordCheck[]>([]);
  readonly notifications = signal<Notice[]>([]);
  readonly feedback = signal<Feedback[]>([]);
  readonly allFeedback = signal<Feedback[]>([]);
  readonly news = signal<NewsItem[]>([]);
  readonly allNews = signal<NewsItem[]>([]);
  readonly members = signal<VaultUser[]>([]);
  readonly admins = signal<VaultUser[]>([]);
  readonly insights = signal<AdminInsights>(EMPTY_INSIGHTS);

  needsSetup(): boolean {
    const role = this.auth.user()?.role;
    if (role === 'admin' || role === 'superadmin') return false;
    return !this.finance();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      await this.refresh();
      this.error.set('');
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Could not load workspace');
      if (error instanceof ApiError && error.status === 401) this.auth.clearSession();
      throw error;
    } finally {
      this.loading.set(false);
    }
  }

  async refresh(): Promise<void> {
    const data = await this.api.get<DashboardPayload>('/dashboard');
    this.apply(data);
    this.loaded.set(true);
  }

  private apply(data: DashboardPayload): void {
    if (data.user) this.auth.patchUser(data.user);
    this.finance.set(data.finance || null);
    this.setupSkipped.set(!!data.setupSkipped);
    this.stats.set(data.stats ? {
      fullName: data.stats.fullName,
      health: num(data.stats.health),
      totalSavings: num(data.stats.totalSavings),
      monthBudget: num(data.stats.monthBudget),
      spent: num(data.stats.spent),
      activeGoals: num(data.stats.activeGoals),
      income: num(data.stats.income),
      expenses: num(data.stats.expenses),
    } : null);
    this.transactions.set(asArray<Tx>(data.transactions));
    this.goals.set(asArray<Goal>(data.goals));
    this.savings.set(asArray<Saving>(data.savings));
    this.budgets.set(asArray<Budget>(data.budgets).map((row) => ({ ...row, status: budgetStatus(row) })));
    this.reports.set(asArray<Report>(data.reports));
    this.affordChecks.set(asArray<AffordCheck>(data.affordChecks));
    this.notifications.set(asArray<Notice>(data.notifications));
    this.feedback.set(asArray<Feedback>(data.feedback));
    this.allFeedback.set(asArray<Feedback>(data.allFeedback?.length ? data.allFeedback : data.feedback));
    this.news.set(asArray<NewsItem>(data.news));
    this.allNews.set(asArray<NewsItem>(data.allNews?.length ? data.allNews : data.news));
    this.members.set(asArray<VaultUser>(data.members));
    this.admins.set(asArray<VaultUser>(data.admins));
    this.insights.set(data.adminInsights ? {
      ...EMPTY_INSIGHTS,
      ...data.adminInsights,
      joinLabels: asArray<string>(data.adminInsights.joinLabels),
      joinValues: asArray<number>(data.adminInsights.joinValues),
      statusLabels: asArray<string>(data.adminInsights.statusLabels),
      statusValues: asArray<number>(data.adminInsights.statusValues),
      setupLabels: asArray<string>(data.adminInsights.setupLabels),
      setupValues: asArray<number>(data.adminInsights.setupValues),
      recentMembers: asArray<VaultUser>(data.adminInsights.recentMembers),
    } : EMPTY_INSIGHTS);
  }
}

function budgetStatus(row: Budget): string {
  const usage = (row.usage || '').toLowerCase();
  if (usage.includes('over')) return 'over';
  if (usage.includes('close') || usage.includes('near')) return 'warn';
  const limit = num(row.limit);
  const spent = num(row.spent);
  if (limit && spent > limit) return 'over';
  if (limit && spent >= limit * 0.8) return 'warn';
  return 'ok';
}
