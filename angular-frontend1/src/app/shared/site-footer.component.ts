import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-site-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="site-footer">
      <span>© 2026 MicroVault · Budgeting, goal savings, and purchase guidance</span>
      <nav>
        @if (signedIn) {
          <a routerLink="/help">Help</a>
          <a routerLink="/feedback">Feedback</a>
        }
        <a routerLink="/terms">Terms</a>
        <a routerLink="/privacy">Privacy</a>
      </nav>
    </footer>
  `,
  styles: `
    .site-footer {
      display: flex;
      flex-wrap: wrap;
      gap: 12px 24px;
      justify-content: space-between;
      align-items: center;
      margin-top: 28px;
      padding: 18px 8px 8px;
      color: var(--muted);
      font-size: 13px;
      border-top: 1px solid var(--line);
    }
    nav { display: flex; gap: 16px; }
    a { color: var(--navy); }
  `,
})
export class SiteFooterComponent {
  @Input() signedIn = false;
}
