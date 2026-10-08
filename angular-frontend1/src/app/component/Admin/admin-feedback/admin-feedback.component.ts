import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { formatDate, queryRows } from '../../../shared/format';
import { Feedback, FeedbackHistory } from '../../../model/models';
import { AdminService } from '../../../service/admin.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-admin-feedback',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './admin-feedback.component.html',
  styleUrl: './admin-feedback.component.scss',
})
export class AdminFeedbackComponent {
  readonly vault = inject(AdminService);
  private readonly dialog = inject(DialogService);
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly statusRow = signal<Feedback | null>(null);
  readonly view = signal<Feedback | null>(null);
  readonly historyOpen = signal(false);
  readonly history = signal<FeedbackHistory[]>([]);
  historyMeta = '';
  nextStatus = 'open';
  readonly openCount = computed(() => this.vault.allFeedback().filter((row) => row.status === 'open').length);
  readonly resolvedCount = computed(() => this.vault.allFeedback().filter((row) => row.status === 'resolved').length);
  readonly page = computed(() => queryRows(this.vault.allFeedback(), this.search(), ['userName', 'userEmail', 'subject', 'message'], { status: this.status(), category: this.category() }, this.pageNo(), 5));

  beginStatus(row: Feedback): void { this.statusRow.set(row); this.nextStatus = row.status; }
  async saveStatus(): Promise<void> {
    const row = this.statusRow();
    if (!row) return;
    try {
      await this.vault.updateFeedback(row.id, { subject: row.subject, category: row.category, message: row.message, status: this.nextStatus });
      this.statusRow.set(null);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not update status');
    }
  }
  async showHistory(row: Feedback): Promise<void> {
    this.historyMeta = `${row.subject} · ${row.userName || ''}`;
    this.history.set(await this.vault.history(row.id));
    this.historyOpen.set(true);
  }
}
