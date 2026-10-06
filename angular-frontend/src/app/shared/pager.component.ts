import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pager',
  standalone: true,
  template: `
    <div class="pagination">
      <button type="button" class="page-btn" [disabled]="page() <= 1" (click)="pageChange.emit(page() - 1)">Prev</button>
      <span class="page-info">Page {{ page() }} of {{ totalPages() }} ({{ total() }} records)</span>
      <button type="button" class="page-btn" [disabled]="page() >= totalPages()" (click)="pageChange.emit(page() + 1)">Next</button>
    </div>
  `,
})
export class PagerComponent {
  readonly page = input(1);
  readonly totalPages = input(1);
  readonly total = input(0);
  readonly pageChange = output<number>();
}
