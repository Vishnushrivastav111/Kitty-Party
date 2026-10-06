import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { Validators, formatDate, queryRows } from '../core/format';
import { NewsItem } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-admin-news',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Broadcast news</h1><p>Post updates that appear for every member at once</p></div>
    <div class="panel">
      <div class="panel-header">
        <h2>News posts</h2>
        <button class="btn btn-primary btn-sm" type="button" (click)="open(null)"><i class="fas fa-plus"></i> Post news</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search news..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="status()" (ngModelChange)="status.set($event); pageNo.set(1)">
          <option value="">All statuses</option><option value="published">Published</option><option value="draft">Draft</option><option value="archived">Archived</option>
        </select>
        <select [ngModel]="priority()" (ngModelChange)="priority.set($event); pageNo.set(1)">
          <option value="">All priorities</option><option value="normal">Normal</option><option value="high">High</option>
        </select>
      </div>
      <div class="feed-stack">
        @if (!page().rows.length) { <div class="empty-state">No news posts</div> }
        @for (row of page().rows; track row.id) {
          <article class="feed-item" [class.is-high]="row.priority === 'high'">
            <strong>{{ row.title }}</strong>
            <p class="feed-meta">{{ date(row.createdAt) }} · {{ row.authorName || 'Admin' }} · {{ row.status }} · {{ row.priority }}</p>
            <p class="feed-text">{{ row.body }}</p>
            <div class="form-actions">
              <button class="btn btn-outline btn-sm" type="button" (click)="open(row)">Edit</button>
              <button class="btn btn-danger btn-sm" type="button" (click)="remove(row)">Delete</button>
            </div>
          </article>
        }
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
    <div class="modal-backdrop" [class.open]="editing()">
      <div class="modal">
        <h3>{{ form.id ? 'Edit news' : 'Post news' }}</h3>
        <form (ngSubmit)="save()" novalidate>
          <div class="form-grid">
            <div class="form-group" style="grid-column:1/-1"><label>Title</label><input name="title" maxlength="140" [(ngModel)]="form.title" /><span class="field-error" [style.display]="errors()['title'] ? 'block' : 'none'">{{ errors()['title'] }}</span></div>
            <div class="form-group" style="grid-column:1/-1"><label>Body</label><textarea name="body" rows="5" [(ngModel)]="form.body"></textarea><span class="field-error" [style.display]="errors()['body'] ? 'block' : 'none'">{{ errors()['body'] }}</span></div>
            <div class="form-group"><label>Priority</label><select name="priority" [(ngModel)]="form.priority"><option value="normal">Normal</option><option value="high">High</option></select></div>
            <div class="form-group"><label>Status</label><select name="status" [(ngModel)]="form.status"><option value="published">Published</option><option value="draft">Draft</option><option value="archived">Archived</option></select></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-outline" (click)="editing.set(false)">Cancel</button>
            <button class="btn btn-primary" [disabled]="busy()">Save</button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class AdminNewsComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  readonly date = formatDate;
  readonly search = signal('');
  readonly status = signal('');
  readonly priority = signal('');
  readonly pageNo = signal(1);
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  form: NewsItem = blank();
  readonly page = computed(() => queryRows(this.vault.allNews(), this.search(), ['title', 'body', 'authorName'], { status: this.status(), priority: this.priority() }, this.pageNo(), 5));

  open(row: NewsItem | null): void {
    this.form = row ? { ...row, priority: row.priority || 'normal', status: row.status || 'published' } : blank();
    this.errors.set({});
    this.editing.set(true);
  }
  async save(): Promise<void> {
    const errors = { title: Validators.required(this.form.title, 'Title'), body: Validators.required(this.form.body, 'Body') };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    const body = { title: this.form.title.trim(), body: this.form.body.trim(), priority: this.form.priority || 'normal', status: this.form.status || 'published' };
    this.busy.set(true);
    try {
      if (this.form.id) await this.vault.updateNews(this.form.id, body); else await this.vault.addNews(body);
      this.editing.set(false);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not save news');
    } finally { this.busy.set(false); }
  }
  async remove(row: NewsItem): Promise<void> {
    if (await this.dialog.confirm('Delete this news post?')) await this.vault.deleteNews(row.id);
  }
}

function blank(): NewsItem {
  return { id: '', title: '', body: '', priority: 'normal', status: 'published' };
}
