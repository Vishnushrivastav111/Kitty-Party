import { Injectable, inject, signal } from '@angular/core';
import { ApiError, ApiService } from './api.service';
import { AuthService } from './auth.service';
import { asArray, num } from './format';
import {
  AdminInsights,
  AffordCheck,
  Budget,
  DashboardPayload,
  DashStats,
  Feedback,
  FeedbackHistory,
  FinanceProfile,
  Goal,
  NewsItem,
  Notice,
  Report,
  Saving,
  Tx,
  VaultUser,
} from './models';

const EMPTY_INSIGHTS: AdminInsights = {
  totalMembers: 0, activeMembers: 0, inactiveMembers: 0, adminCount: 0,
  setupDone: 0, setupPending: 0, feedbackTotal: 0, feedbackOpen: 0,
  newsTotal: 0, publishedNews: 0, joinLabels: [], joinValues: [],
  statusLabels: ['Active', 'Inactive'], statusValues: [0, 0],
  setupLabels: ['Setup done', 'Pending'], setupValues: [0, 0], recentMembers: [],
};

@Injectable({ providedIn: 'root' })
export class VaultService {
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

  async saveFinance(body: Record<string, unknown>): Promise<FinanceProfile> {
    const row = await this.api.put<FinanceProfile>('/finance', body);
    await this.refresh();
    return row;
  }

  async skipSetup(): Promise<void> {
    await this.api.post('/finance/skip', {});
    await this.refresh();
  }

  async addTx(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/transactions', body);
    await this.refresh();
  }

  async updateTx(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/transactions/${id}`, body);
    await this.refresh();
  }

  async deleteTx(id: string): Promise<void> {
    await this.api.delete(`/transactions/${id}`);
    await this.refresh();
  }

  async clearTx(): Promise<void> {
    await this.api.delete('/transactions');
    await this.refresh();
  }

  async addGoal(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/goals', body);
    await this.refresh();
  }

  async updateGoal(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/goals/${id}`, body);
    await this.refresh();
  }

  async deleteGoal(id: string): Promise<void> {
    await this.api.delete(`/goals/${id}`);
    await this.refresh();
  }

  async clearGoals(): Promise<void> {
    await this.api.delete('/goals');
    await this.refresh();
  }

  async addSaving(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/savings', body);
    await this.refresh();
  }

  async updateSaving(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/savings/${id}`, body);
    await this.refresh();
  }

  async deleteSaving(id: string): Promise<void> {
    await this.api.delete(`/savings/${id}`);
    await this.refresh();
  }

  async clearSavings(): Promise<void> {
    await this.api.delete('/savings');
    await this.refresh();
  }

  async addBudget(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/budgets', body);
    await this.refresh();
  }

  async updateBudget(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/budgets/${id}`, body);
    await this.refresh();
  }

  async deleteBudget(id: string): Promise<void> {
    await this.api.delete(`/budgets/${id}`);
    await this.refresh();
  }

  async clearBudgets(): Promise<void> {
    await this.api.delete('/budgets');
    await this.refresh();
  }

  async generateReport(body: { fromDate: string; toDate: string; type: string }): Promise<Report> {
    const row = await this.api.post<Report>('/reports/generate', body);
    await this.refresh();
    return row;
  }

  async deleteReport(id: string): Promise<void> {
    await this.api.delete(`/reports/${id}`);
    await this.refresh();
  }

  async clearReports(): Promise<void> {
    await this.api.delete('/reports');
    await this.refresh();
  }

  async checkAfford(body: Record<string, unknown>): Promise<AffordCheck> {
    const row = await this.api.post<AffordCheck>('/affordability/check', body);
    await this.refresh();
    return row;
  }

  async deleteAfford(id: string): Promise<void> {
    await this.api.delete(`/affordability/${id}`);
    await this.refresh();
  }

  async clearAfford(): Promise<void> {
    await this.api.delete('/affordability');
    await this.refresh();
  }

  async markRead(id: string): Promise<void> {
    await this.api.put(`/notifications/${id}/read`, {});
    await this.refresh();
  }

  async markAllRead(): Promise<void> {
    await this.api.put('/notifications/read-all', {});
    await this.refresh();
  }

  async deleteNotice(id: string): Promise<void> {
    await this.api.delete(`/notifications/${id}`);
    await this.refresh();
  }

  async clearNotices(): Promise<void> {
    await this.api.delete('/notifications');
    await this.refresh();
  }

  async addFeedback(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/feedback', body);
    await this.refresh();
  }

  async updateFeedback(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/feedback/${id}`, body);
    await this.refresh();
  }

  async deleteFeedback(id: string): Promise<void> {
    await this.api.delete(`/feedback/${id}`);
    await this.refresh();
  }

  history(id: string): Promise<FeedbackHistory[]> {
    return this.api.get<FeedbackHistory[]>(`/feedback/${id}/history`).then((rows) => asArray<FeedbackHistory>(rows));
  }

  async addNews(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/admin/news', body);
    await this.refresh();
  }

  async updateNews(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/admin/news/${id}`, body);
    await this.refresh();
  }

  async deleteNews(id: string): Promise<void> {
    await this.api.delete(`/admin/news/${id}`);
    await this.refresh();
  }

  async saveMember(id: string | null, body: Record<string, unknown>): Promise<void> {
    if (id) {
      await this.api.put(`/admin/users/${id}`, { ...body, role: 'user' });
    } else {
      const created = await this.auth.register({
        fullName: String(body['fullName'] || ''),
        email: String(body['email'] || ''),
        phone: String(body['phone'] || ''),
        password: String(body['password'] || ''),
        confirmPassword: String(body['password'] || ''),
      });
      if (!created.ok || !created.user) throw new Error(created.message || 'Could not create user');
      await this.api.put(`/admin/users/${created.user.id}`, {
        fullName: body['fullName'],
        email: body['email'],
        phone: body['phone'],
        status: body['status'] || 'active',
        role: 'user',
      });
    }
    await this.refresh();
  }

  async deleteMember(id: string): Promise<void> {
    await this.api.delete(`/admin/users/${id}`);
    await this.refresh();
  }

  async saveAdmin(id: string | null, body: Record<string, unknown>): Promise<void> {
    const payload = { ...body, role: 'admin' };
    if (id) await this.api.put(`/admin/admins/${id}`, payload);
    else await this.api.post('/admin/admins', payload);
    await this.refresh();
  }

  async deleteAdmin(id: string): Promise<void> {
    await this.api.delete(`/admin/admins/${id}`);
    await this.refresh();
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
