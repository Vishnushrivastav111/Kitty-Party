import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-graph-pager',
  standalone: true,
  templateUrl: './graph-pager.component.html',
  styleUrl: './graph-pager.component.scss',
})
export class GraphPagerComponent {
  readonly index = input(0);
  readonly total = input(1);
  readonly title = input('');
  readonly pageChange = output<number>();

  previous(): void {
    if (this.index() > 0) this.pageChange.emit(this.index() - 1);
  }

  next(): void {
    if (this.index() < this.total() - 1) this.pageChange.emit(this.index() + 1);
  }

  onKey(event: KeyboardEvent): void {
    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      this.previous();
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      this.next();
    }
  }
}
