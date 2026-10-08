import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { formatDate, queryRows } from '../../../shared/format';
import { Notice } from '../../../model/models';
import { UserService } from '../../../service/user.service';
import { PagerComponent } from '../../../shared/pager.component';

interface NoticeRow extends Omit<Notice, 'read'> {
  read: string;
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export class NotificationsComponent {
  readonly vault = inject(UserService);
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
