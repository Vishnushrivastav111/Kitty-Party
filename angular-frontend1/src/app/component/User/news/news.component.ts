import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { formatDate, queryRows } from '../../../shared/format';
import { NewsItem } from '../../../model/models';
import { UserService } from '../../../service/user.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-news',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './news.component.html',
  styleUrl: './news.component.scss',
})
export class NewsComponent {
  private readonly vault = inject(UserService);
  readonly date = formatDate;
  readonly search = signal('');
  readonly priority = signal('');
  readonly pageNo = signal(1);
  readonly selected = signal<NewsItem | null>(null);
  readonly page = computed(() => queryRows(this.vault.news(), this.search(), ['title', 'body', 'authorName'], { priority: this.priority() }, this.pageNo(), 5));
}
