import { NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { Validators, formatDate, queryRows } from '../core/format';
import { Feedback, FeedbackHistory } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [FormsModule, PagerComponent, NgClass],
  template: `
    <div class="page-header"><h1>Feedback</h1><p>Send feedback, update it anytime, and keep a full history of your submissions</p></div>
    <div class="panel">
      <div class="panel-header">
        <h2>My feedback history</h2>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Send feedback</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search feedback..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="status()" (ngModelChange)="status.set($event); pageNo.set(1)">
          <option value="">All statuses</option><option value="open">Open</option><option value="in-review">In review</option><option value="resolved">Resolved</option><option value="closed">Closed</option>
        </select>
        <select [ngModel]="category()" (ngModelChange)="category.set($event); pageNo.set(1)">
          <option value="">All categories</option>
          @for (item of categories; track item) { <option [value]="item">{{ item }}</option> }
        </select>
      </div>
      <div class="feed-stack">
        @if (!page().rows.length) { <div class="empty-state">No feedback yet</div> }
        @for (row of page().rows; track row.id) {
          <article class="feed-item" [class.is-open]="row.status === 'open'">
            <strong>{{ row.subject }}</strong>
            <p class="feed-meta">{{ date(row.createdAt) }} · {{ row.category }} · <span class="badge" [ngClass]="badge(row.status)">{{ row.status }}</span></p>
            <p class="feed-text">{{ row.message }}</p>
            <div class="form-actions">
              <button class="btn btn-outline btn-sm" type="button" (click)="open(row)">Edit</button>
              <button class="btn btn-outline btn-sm" type="button" (click)="showHistory(row)">History</button>
              <button class="btn btn-danger btn-sm" type="button" (click)="remove(row)">Delete</button>
            </div>
          </article>
        }
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
    <div class="modal-backdrop" [class.open]="editing()">
      <div class="modal">
        <h3>{{ form.id ? 'Update feedback' : 'Send feedback' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group"><label>Subject</label><input name="subject" maxlength="120" [(ngModel)]="form.subject" /><span class="field-error" [style.display]="errors()['subject'] ? 'block' : 'none'">{{ errors()['subject'] }}</span></div>
            <div class="form-group"><label>Category</label>
              <select name="category" [(ngModel)]="form.category"><option value="">Select</option>@for (item of categories; track item) { <option [value]="item">{{ item }}</option> }</select>
              <span class="field-error" [style.display]="errors()['category'] ? 'block' : 'none'">{{ errors()['category'] }}</span>
            </div>
            <div class="form-group" style="grid-column:1/-1"><label>Message</label><textarea name="message" rows="5" maxlength="1000" [(ngModel)]="form.message"></textarea><span class="field-error" [style.display]="errors()['message'] ? 'block' : 'none'">{{ errors()['message'] }}</span></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-outline" (click)="editing.set(false)">Cancel</button>
            <button class="btn btn-primary" [disabled]="busy()">Save</button>
          </div>
        </form>
      </div>
    </div>
    <div class="modal-backdrop" [class.open]="historyOpen()">
      <div class="modal">
        <h3>Feedback history</h3>
        <p style="color:var(--muted);margin-bottom:12px">{{ historyMeta }}</p>
        <ul class="timeline">
          @if (!history().length) { <li>No history recorded yet.</li> }
          @for (item of history(); track $index) { <li><strong>{{ item.action }}</strong> · {{ item.by || 'You' }} · {{ item.at }}<div>{{ item.note }}</div></li> }
        </ul>
        <div class="modal-actions"><button type="button" class="btn btn-outline" (click)="historyOpen.set(false)">Close</button></div>
      </div>
    </div>
  `,
})
export class FeedbackComponent {
  private readonly vault = inject(VaultService);
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
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save feedback');
    } finally { this.busy.set(false); }
  }
  async showHistory(row: Feedback): Promise<void> {
    this.historyMeta = row.subject;
    this.history.set(await this.vault.history(row.id));
    this.historyOpen.set(true);
  }
  async remove(row: Feedback): Promise<void> {
    if (await this.dialog.confirm('Delete this feedback?')) await this.vault.deleteFeedback(row.id);
  }
}

function blank(): Feedback {
  return { id: '', subject: '', category: '', message: '', status: 'open' };
}
