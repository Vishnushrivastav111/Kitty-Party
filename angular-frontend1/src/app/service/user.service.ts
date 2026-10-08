import { Injectable, inject } from '@angular/core';
import { asArray } from '../shared/format';
import { FeedbackHistory } from '../model/models';
import { ApiService } from './api.service';
import { WorkspaceService } from './workspace.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = inject(ApiService);
  private readonly workspace = inject(WorkspaceService);

  readonly loaded = this.workspace.loaded;
  readonly loading = this.workspace.loading;
  readonly error = this.workspace.error;
  readonly notifications = this.workspace.notifications;
  readonly feedback = this.workspace.feedback;
  readonly news = this.workspace.news;

  load(): Promise<void> {
    return this.workspace.load();
  }

  async markRead(id: string): Promise<void> {
    await this.api.put(`/notifications/${id}/read`, {});
    await this.workspace.refresh();
  }

  async markAllRead(): Promise<void> {
    await this.api.put('/notifications/read-all', {});
    await this.workspace.refresh();
  }

  async deleteNotice(id: string): Promise<void> {
    await this.api.delete(`/notifications/${id}`);
    await this.workspace.refresh();
  }

  async clearNotices(): Promise<void> {
    await this.api.delete('/notifications');
    await this.workspace.refresh();
  }

  async addFeedback(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/feedback', body);
    await this.workspace.refresh();
  }

  async updateFeedback(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/feedback/${id}`, body);
    await this.workspace.refresh();
  }

  async deleteFeedback(id: string): Promise<void> {
    await this.api.delete(`/feedback/${id}`);
    await this.workspace.refresh();
  }

  history(id: string): Promise<FeedbackHistory[]> {
    return this.api.get<FeedbackHistory[]>(`/feedback/${id}/history`).then((rows) => asArray<FeedbackHistory>(rows));
  }
}
