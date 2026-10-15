import { Component } from '@angular/core';

/** Single source for the Terms & Conditions text, used by the registration dialog and the /terms page. */
@Component({
  selector: 'app-terms-content',
  standalone: true,
  template: `
    <p><strong>MicroVault</strong> is a personal finance platform for budgeting, expense tracking, goal-based savings, affordability checks, and financial guidance. By creating an account you agree to the terms below.</p>

    <h4>1. Accurate information</h4>
    <p>I agree to provide correct, complete, and current information when I register and when I enter my income, expenses, EMIs, savings, budgets, and goals. I understand that every plan, limit, and suggestion in MicroVault is calculated from what I enter, so inaccurate data produces misleading results.</p>

    <h4>2. My account and credentials</h4>
    <p>I will keep my password confidential, will not share my account, and am responsible for all activity under it. I will tell the platform if I suspect unauthorised access.</p>

    <h4>3. Personal use</h4>
    <p>I will use MicroVault only to manage my own finances. I will not enter another person's financial data without their permission, attempt to access other users' data, or interfere with the service.</p>

    <h4>4. Planning tool, not financial advice</h4>
    <p>Budgets, goal feasibility, affordability results, and insights are planning guidance based on my inputs. They are not a guarantee of financial safety and are not regulated banking, investment, tax, or legal advice. Investment guidance is educational; MicroVault makes no promise of returns. I remain responsible for my own financial decisions.</p>

    <h4>5. No real money movement</h4>
    <p>MicroVault records and plans. It does not move money, make payments, place investment orders, or connect to my bank. Savings contributions, transfers, and goals are records that I maintain myself.</p>

    <h4>6. My data and privacy</h4>
    <p>My profile and financial records are stored by the MicroVault services and are visible only to me and to administrators who need access to operate the platform. I can review and update my financial profile at any time. See the Privacy page for details.</p>

    <h4>7. Notifications</h4>
    <p>MicroVault may create in-app notifications, such as when my spending reaches a budget threshold. These are informational and do not replace my own monitoring of my accounts.</p>

    <h4>8. Account closure and changes</h4>
    <p>I may request account closure by contacting an administrator. The platform may suspend accounts that provide false information or misuse the service, and may update these terms; continued use means I accept the updated terms.</p>

    <h4>9. Acceptance</h4>
    <p>By ticking the acceptance box I confirm that I have read and understood these Terms &amp; Conditions.</p>
  `,
  styles: [`
    :host { display: block; line-height: 1.6; color: var(--slate); font-size: 14px; }
    h4 { color: var(--navy); margin: 14px 0 4px; font-size: 15px; }
    p { margin: 0 0 8px; }
  `],
})
export class TermsContentComponent {}
