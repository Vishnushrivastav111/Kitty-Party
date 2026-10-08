import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './terms.component.html',
  styleUrl: './terms.component.scss',
})
export class TermsComponent {
  readonly unlocked = signal(false);
  readonly accepted = signal(false);

  onScroll(box: HTMLElement): void {
    if (box.scrollTop + box.clientHeight >= box.scrollHeight - 8) this.unlocked.set(true);
  }

  accept(): void {
    localStorage.setItem('mv_termsAccepted', 'true');
    history.back();
  }
}
