import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pager',
  standalone: true,
  templateUrl: './pager.component.html',
  styleUrl: './pager.component.scss',
})
export class PagerComponent {
  readonly page = input(1);
  readonly totalPages = input(1);
  readonly total = input(0);
  readonly pageChange = output<number>();
}
