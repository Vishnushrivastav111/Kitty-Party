import { Injectable, inject } from '@angular/core';
import { asArray } from '../shared/format';
import { FeedbackHistory } from '../model/models';
import { ApiService } from './api.service';
import { AuthService } from '../service/auth.service';
import { WorkspaceService } from './workspace.service';

export interface ApprovalRequest {
  id: string;
  type: 'TRANSACTION_DELETE' | 'FINANCIAL_SETUP' | string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | string;
  userId?: string;
  userName?: string;
  userEmail?: string;
  requestedAt?: string;
  reason?: string;
  transaction?: Record<string, unknown>;
  financialSetup?: Record<string, unknown>;
  details?: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly workspace = inject(WorkspaceService);

  readonly allFeedback = this.workspace.allFeedback;
  readonly allNews = this.workspace.allNews;
  readonly members = this.workspace.members;
  readonly admins = this.workspace.admins;
  readonly insights = this.workspace.insights;

  async getApprovals(): Promise<ApprovalRequest[]> {
    const rows = await this.api.get<ApprovalRequest[]>('/admin/approvals');
    return asArray<ApprovalRequest>(rows);
  }

  async approveRequest(id: string): Promise<void> {
    await this.api.post(`/admin/approvals/${id}/approve`, {});
  }

  async rejectRequest(id: string, reason = ''): Promise<void> {
    await this.api.post(`/admin/approvals/${id}/reject`, { reason });
  }

  async requestTransactionDelete(id: string): Promise<void> {
    await this.api.post(`/transactions/${id}/delete-request`, {});
  }

  async requestFinancialSetupApproval(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/finance/setup-request', body);
  }

  async updateFeedback(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/feedback/${id}`, body);
    await this.workspace.refresh();
  }

  history(id: string): Promise<FeedbackHistory[]> {
    return this.api.get<FeedbackHistory[]>(`/feedback/${id}/history`).then((rows) => asArray<FeedbackHistory>(rows));
  }

  async addNews(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/admin/news', body);
    await this.workspace.refresh();
  }

  async updateNews(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/admin/news/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteNews(id: string): Promise<void> {
    await this.api.delete(`/admin/news/${id}`);
    await this.workspace.refresh();
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
    await this.workspace.refresh();
  }

  async deleteMember(id: string): Promise<void> {
    await this.api.delete(`/admin/users/${id}`);
    await this.workspace.refresh();
  }

  async saveAdmin(id: string | null, body: Record<string, unknown>): Promise<void> {
    const payload = { ...body, role: 'admin' };
    if (id) await this.api.put(`/admin/admins/${id}`, payload);
    else await this.api.post('/admin/admins', payload);
    await this.workspace.refresh();
  }

  async deleteAdmin(id: string): Promise<void> {
    await this.api.delete(`/admin/admins/${id}`);
    await this.workspace.refresh();
  }
}
