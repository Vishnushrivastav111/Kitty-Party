import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { formatDate, queryRows } from '../core/format';
import { Notice } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

interface NoticeRow extends Omit<Notice, 'read'> {
  read: string;
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Notifications</h1><p>Alerts for savings, budgets, goals, and system updates</p></div>
    <div class="panel">
      <div class="panel-header">
        <h2>Inbox</h2>
        <button class="btn btn-outline btn-sm" type="button" (click)="vault.markAllRead()">Mark all read</button>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search notifications..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="type()" (ngModelChange)="type.set($event); pageNo.set(1)">
          <option value="">All types</option><option value="info">Info</option><option value="success">Success</option><option value="warning">Warning</option><option value="danger">Alert</option>
        </select>
        <select [ngModel]="read()" (ngModelChange)="read.set($event); pageNo.set(1)">
          <option value="">All</option><option value="false">Unread</option><option value="true">Read</option>
        </select>
      </div>
      <div class="feed-stack">
        @if (!page().rows.length) { <div class="empty-state">No notifications</div> }
        @for (row of page().rows; track row.id) {
          <article class="feed-item" [class.is-unread]="row.read !== 'true'" (click)="open(row)">
            <strong>{{ row.title }}</strong>
            <p class="feed-text">{{ row.message }}</p>
            <p class="feed-meta">{{ date(row.date || row.createdAt) }} · {{ row.type }} · {{ row.read === 'true' ? 'Read' : 'Unread' }}</p>
            <button class="btn btn-danger btn-sm" type="button" (click)="remove(row, $event)"><i class="fas fa-trash"></i></button>
          </article>
        }
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
  `,
})
export class NotificationsComponent {
  readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  readonly date = formatDate;
  readonly search = signal('');
  readonly type = signal('');
  readonly read = signal('');
  readonly pageNo = signal(1);
  readonly rows = computed((): NoticeRow[] => this.vault.notifications().map((row) => ({ ...row, read: String(row.read) })));
  readonly page = computed(() => queryRows(this.rows(), this.search(), ['title', 'message', 'type'], { type: this.type(), read: this.read() }, this.pageNo(), 6));

  async open(row: NoticeRow): Promise<void> {
    if (row.read !== 'true') await this.vault.markRead(row.id);
  }
  async remove(row: NoticeRow, event: Event): Promise<void> {
    event.stopPropagation();
    if (await this.dialog.confirm('Delete this notification?')) await this.vault.deleteNotice(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Clear every notification?')) await this.vault.clearNotices();
  }
}
