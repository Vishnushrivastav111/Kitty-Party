import { Injectable, inject } from '@angular/core';
import { AffordCheck, FinanceProfile, Report } from '../model/models';
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

  load(): Promise<void> {
    return this.workspace.load();
  }

  needsSetup(): boolean {
    return this.workspace.needsSetup();
  }

  async saveFinance(body: Record<string, unknown>): Promise<FinanceProfile> {
    const row = await this.api.put<FinanceProfile>('/finance', body);
    await this.workspace.refresh();
    return row;
  }

  async skipSetup(): Promise<void> {
    await this.api.post('/finance/skip', {});
    await this.workspace.refresh();
  }

  async addTx(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/transactions', body);
    await this.workspace.refresh();
  }

  async updateTx(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/transactions/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteTx(id: string): Promise<void> {
    await this.api.delete(`/transactions/${id}`);
    await this.workspace.refresh();
  }

  async clearTx(): Promise<void> {
    await this.api.delete('/transactions');
    await this.workspace.refresh();
  }

  async addGoal(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/goals', body);
    await this.workspace.refresh();
  }

  async updateGoal(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/goals/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteGoal(id: string): Promise<void> {
    await this.api.delete(`/goals/${id}`);
    await this.workspace.refresh();
  }

  async clearGoals(): Promise<void> {
    await this.api.delete('/goals');
    await this.workspace.refresh();
  }

  async addSaving(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/savings', body);
    await this.workspace.refresh();
  }

  async updateSaving(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/savings/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteSaving(id: string): Promise<void> {
    await this.api.delete(`/savings/${id}`);
    await this.workspace.refresh();
  }

  async clearSavings(): Promise<void> {
    await this.api.delete('/savings');
    await this.workspace.refresh();
  }

  async addBudget(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/budgets', body);
    await this.workspace.refresh();
  }

  async updateBudget(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/budgets/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteBudget(id: string): Promise<void> {
    await this.api.delete(`/budgets/${id}`);
    await this.workspace.refresh();
  }

  async clearBudgets(): Promise<void> {
    await this.api.delete('/budgets');
    await this.workspace.refresh();
  }

  async generateReport(body: { fromDate: string; toDate: string; type: string }): Promise<Report> {
    const row = await this.api.post<Report>('/reports/generate', body);
    await this.workspace.refresh();
    return row;
  }

  async deleteReport(id: string): Promise<void> {
    await this.api.delete(`/reports/${id}`);
    await this.workspace.refresh();
  }

  async clearReports(): Promise<void> {
    await this.api.delete('/reports');
    await this.workspace.refresh();
  }

  async checkAfford(body: Record<string, unknown>): Promise<AffordCheck> {
    const row = await this.api.post<AffordCheck>('/affordability/check', body);
    await this.workspace.refresh();
    return row;
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
