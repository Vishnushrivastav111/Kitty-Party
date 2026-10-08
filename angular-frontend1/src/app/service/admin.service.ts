import { Injectable, inject } from '@angular/core';
import { asArray } from '../shared/format';
import { FeedbackHistory } from '../model/models';
import { ApiService } from './api.service';
import { AuthService } from '../service/auth.service';
import { WorkspaceService } from './workspace.service';

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
