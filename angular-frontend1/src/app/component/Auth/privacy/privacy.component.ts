import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteFooterComponent } from '../../../shared/site-footer.component';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [RouterLink, SiteFooterComponent],
  template: `
    <main class="auth-page" style="display:block;padding:32px 20px 16px;max-width:760px;margin:0 auto">
      <a routerLink="/" class="brand"><i class="fas fa-vault"></i> MicroVault</a>
      <h1>Privacy policy</h1>
      <p>MicroVault stores the financial profile, transactions, budgets, savings, goals, and affordability checks you enter so those features can calculate your plan.</p>
      <p>Your records are available only to your signed-in account, and to administrators for account support such as feedback and user status. The application does not sell financial data and does not place investment orders.</p>
      <p>Guidance on this site is educational. It is calculated from the figures you save, and it is not a promise of returns or a recommendation to buy a specific investment.</p>
      <p><a routerLink="/terms">Terms</a> · <a routerLink="/register">Create an account</a></p>
      <app-site-footer />
    </main>
  `,
})
export class PrivacyComponent {}
