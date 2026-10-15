import { Injectable, computed, inject } from '@angular/core';
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
  readonly refreshWarning = this.workspace.refreshWarning;
  readonly affordabilityStale = this.workspace.affordabilityStale;
  readonly planStale = this.workspace.planStale;

  load(): Promise<void> {
    return this.workspace.load();
  }

  retryRefresh(): Promise<void> {
    return this.workspace.refresh();
  }

  /** Number of unread notifications, shown as the red circle in the sidebar. */
  readonly unreadCount = computed(() => this.workspace.notifications().filter((row) => !row.read).length);

  refreshNotifications(): Promise<void> {
    return this.workspace.refreshNotifications();
  }

  async markRead(id: string): Promise<void> {
    this.workspace.markReadLocally(id);
    try {
      await this.api.put(`/notifications/${id}/read`, {});
    } finally {
      await this.workspace.refreshNotifications().catch(() => undefined);
    }
  }

  async markAllRead(): Promise<void> {
    this.workspace.markReadLocally();
    try {
      await this.api.put('/notifications/read-all', {});
    } finally {
      await this.workspace.refreshNotifications().catch(() => undefined);
    }
  }

  async deleteNotice(id: string): Promise<void> {
    await this.api.delete(`/notifications/${id}`);
    await this.workspace.refreshNotifications();
  }

  async clearNotices(): Promise<void> {
    await this.api.delete('/notifications');
    await this.workspace.refreshNotifications();
  }

  async addFeedback(body: Record<string, unknown>): Promise<void> {
    await this.api.post('/feedback', body);
    await this.workspace.refresh();
  }

  async updateFeedback(id: string, body: Record<string, unknown>): Promise<void> {
    await this.api.put(`/feedback/${id}`, body);
    await this.workspace.refresh();
  }

  history(id: string): Promise<FeedbackHistory[]> {
    return this.api.get<FeedbackHistory[]>(`/feedback/${id}/history`).then((rows) => asArray<FeedbackHistory>(rows));
  }
}
