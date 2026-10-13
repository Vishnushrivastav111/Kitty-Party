import { NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { parseApiError } from '../../../shared/api-error';
import { DialogService } from '../../../shared/dialog.service';
import { assignError } from '../../../shared/field-state';
import { Validators, formatDate, queryRows } from '../../../shared/format';
import { Feedback, FeedbackHistory } from '../../../model/models';
import { UserService } from '../../../service/user.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [FormsModule, PagerComponent, NgClass],
  templateUrl: './feedback.component.html',
  styleUrl: './feedback.component.scss',
})
export class FeedbackComponent {
  private readonly vault = inject(UserService);
  private readonly dialog = inject(DialogService);
  readonly categories = ['General', 'Bug', 'Feature', 'UI', 'Other'];
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly category = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly historyOpen = signal(false);
  readonly history = signal<FeedbackHistory[]>([]);
  historyMeta = '';
  form: Feedback = blank();
  readonly page = computed(() => queryRows(this.vault.feedback(), this.search(), ['subject', 'message', 'category'], { status: this.status(), category: this.category() }, this.pageNo(), 5));

  badge(status: string): string {
    if (status === 'resolved') return 'badge-success';
    if (status === 'in-review') return 'badge-warning';
    if (status === 'closed') return 'badge-neutral';
    return 'badge-info';
  }
  open(row: Feedback | null): void {
    this.form = row ? { ...row } : blank();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = {
      subject: Validators.required(this.form.subject, 'Subject'),
      category: Validators.required(this.form.category, 'Category'),
      message: Validators.required(this.form.message, 'Message'),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { subject: this.form.subject.trim(), category: this.form.category, message: this.form.message.trim(), status: this.form.status || 'open' };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateFeedback(this.form.id, body); else await this.vault.addFeedback(body);
      this.editing.set(false);
    } catch (error) {
      const parsed = parseApiError(error);
      this.errors.set({ ...errors, ...parsed.fieldErrors, form: parsed.message });
    } finally { this.busy.set(false); }
  }
  touch(field: string, message: string): void { this.errors.update((current) => assignError(current, field, message)); }
  subjectOk(value: string): string { return Validators.required(value, 'Subject'); }
  messageOk(value: string): string { return Validators.required(value, 'Message'); }
  async showHistory(row: Feedback): Promise<void> {
    try {
      this.historyMeta = row.subject;
      this.history.set(await this.vault.history(row.id));
      this.historyOpen.set(true);
    } catch (error) {
      await this.dialog.notice(parseApiError(error).message, 'Feedback history');
    }
  }
}

function blank(): Feedback {
  return { id: '', subject: '', category: '', message: '', status: 'open' };
}
