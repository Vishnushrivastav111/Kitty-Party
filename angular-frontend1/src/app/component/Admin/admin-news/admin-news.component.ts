import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { Validators, formatDate, queryRows } from '../../../shared/format';
import { NewsItem } from '../../../model/models';
import { AdminService } from '../../../service/admin.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-admin-news',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './admin-news.component.html',
  styleUrl: './admin-news.component.scss',
})
export class AdminNewsComponent {
  private readonly vault = inject(AdminService);
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
