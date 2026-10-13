import { Injectable, inject } from '@angular/core';
import { AffordCheck, FinanceProfile, Report } from '../model/models';
import { parseApiError } from '../shared/api-error';
import { DeletionRequestResult, transactionDeletionRequest } from '../shared/finance-rules';
import { ApiService } from './api.service';
import { WorkspaceService } from './workspace.service';

@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly api = inject(ApiService);
  private readonly workspace = inject(WorkspaceService);

  readonly loaded = this.workspace.loaded;
  readonly loading = this.workspace.loading;
  readonly error = this.workspace.error;
  readonly finance = this.workspace.finance;
  readonly setupSkipped = this.workspace.setupSkipped;
  readonly stats = this.workspace.stats;
  readonly transactions = this.workspace.transactions;
  readonly goals = this.workspace.goals;
  readonly savings = this.workspace.savings;
  readonly budgets = this.workspace.budgets;
  readonly reports = this.workspace.reports;
  readonly affordChecks = this.workspace.affordChecks;
  readonly refreshWarning = this.workspace.refreshWarning;
  readonly affordabilityStale = this.workspace.affordabilityStale;
  readonly planStale = this.workspace.planStale;

  load(): Promise<void> {
    return this.workspace.load();
  }

  needsSetup(): boolean {
    return this.workspace.needsSetup();
  }

  acknowledgePlan(): void {
    this.workspace.acknowledgePlan();
  }

  async saveFinance(body: Record<string, unknown>): Promise<FinanceProfile> {
    const row = await this.api.put<FinanceProfile>('/finance', body);
    await this.refreshAfterWrite();
    return row;
  }

  async skipSetup(): Promise<void> {
    await this.api.post('/finance/skip', {});
    await this.refreshAfterWrite();
  }

  async addTx(body: Record<string, unknown>): Promise<string> {
    await this.api.post('/transactions', body);
    return this.refreshAfterWrite();
  }

  async updateTx(id: string, body: Record<string, unknown>): Promise<string> {
    await this.api.put(`/transactions/${id}`, body);
    return this.refreshAfterWrite();
  }

  requestTransactionDeletion(_id: string): DeletionRequestResult {
    return transactionDeletionRequest();
  }

  async addGoal(body: Record<string, unknown>): Promise<string> {
    await this.api.post('/goals', body);
    return this.refreshAfterWrite();
  }

  async updateGoal(id: string, body: Record<string, unknown>): Promise<string> {
    await this.api.put(`/goals/${id}`, body);
    return this.refreshAfterWrite();
  }

  async deleteGoal(id: string): Promise<string> {
    await this.api.delete(`/goals/${id}`);
    return this.refreshAfterWrite();
  }

  async clearGoals(): Promise<string> {
    await this.api.delete('/goals');
    return this.refreshAfterWrite();
  }

  async addSaving(body: Record<string, unknown>): Promise<string> {
    await this.api.post('/savings', body);
    return this.refreshAfterWrite();
  }

  async updateSaving(id: string, body: Record<string, unknown>): Promise<string> {
    await this.api.put(`/savings/${id}`, body);
    return this.refreshAfterWrite();
  }

  async deleteSaving(id: string): Promise<string> {
    await this.api.delete(`/savings/${id}`);
    return this.refreshAfterWrite();
  }

  async clearSavings(): Promise<string> {
    await this.api.delete('/savings');
    return this.refreshAfterWrite();
  }

  async addBudget(body: Record<string, unknown>): Promise<string> {
    await this.api.post('/budgets', body);
    return this.refreshAfterWrite();
  }

  async updateBudget(id: string, body: Record<string, unknown>): Promise<string> {
    await this.api.put(`/budgets/${id}`, body);
    return this.refreshAfterWrite();
  }

  async deleteBudget(id: string): Promise<string> {
    await this.api.delete(`/budgets/${id}`);
    return this.refreshAfterWrite();
  }

  async clearBudgets(): Promise<string> {
    await this.api.delete('/budgets');
    return this.refreshAfterWrite();
  }

  async generateReport(body: { fromDate: string; toDate: string; type: string }): Promise<Report> {
    const row = await this.api.post<Report>('/reports/generate', body);
    await this.refreshAfterWrite();
    return row;
  }

  async checkAfford(body: Record<string, unknown>): Promise<AffordCheck> {
    const row = await this.api.post<AffordCheck>('/affordability/check', body);
    await this.refreshQuietly();
    this.workspace.acknowledgeAffordability();
    return row;
  }

  async monthlySummary(userId: string): Promise<Record<string, unknown>> {
    return this.api.get<Record<string, unknown>>(`/finance/profile/${userId}/summary`);
  }

  private async refreshAfterWrite(): Promise<string> {
    const warning = await this.refreshQuietly();
    this.workspace.markDependentsStale();
    return warning;
  }

  private async refreshQuietly(): Promise<string> {
    try {
      await this.workspace.refresh();
      return '';
    } catch (error) {
      const message = parseApiError(error).message;
      this.workspace.refreshWarning.set(message);
      return message;
    }
  }

  async deleteAfford(id: string): Promise<void> {
    await this.api.delete(`/affordability/${id}`);
    await this.workspace.refresh();
  }

  async clearAfford(): Promise<void> {
    await this.api.delete('/affordability');
    await this.workspace.refresh();
  }
}
