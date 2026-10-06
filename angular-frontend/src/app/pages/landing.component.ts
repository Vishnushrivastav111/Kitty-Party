import { Component, HostListener, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  styleUrl: './landing.component.css',
  template: `
    <nav class="nav" [class.scrolled]="scrolled()">
      <div class="brand"><i class="fas fa-vault"></i> MicroVault</div>
      <div class="nav-actions">
        <a class="btn btn-outline" routerLink="/login">Login</a>
        <a class="btn btn-solid" routerLink="/register">Register</a>
      </div>
    </nav>

    <section class="hero">
      <div>
        <div class="eyebrow"><i class="fas fa-shield-halved"></i> Business-ready personal finance</div>
        <h1>Control money with <em>clarity</em> and confidence</h1>
        <p>
          Track savings, budgets, goals, affordability, and reports in one polished workspace —
          with role-based access for members, admins, and super administrators.
        </p>
        <div class="hero-ctas">
          <a class="btn btn-solid" routerLink="/register"><i class="fas fa-rocket"></i> Start free</a>
          <a class="btn btn-outline" routerLink="/login"><i class="fas fa-right-to-bracket"></i> Sign in</a>
        </div>
      </div>
      <div class="preview" (mousemove)="tilt($event)" (mouseleave)="resetTilt($event)">
        <div class="preview-top">
          <strong>Portfolio snapshot</strong>
          <span class="pill"><i class="fas fa-arrow-trend-up"></i> Healthy</span>
        </div>
        <div class="big-num">₹2,45,000</div>
        <div class="metric"><span>Savings</span><span style="color:#0d9488">₹1,20,000</span></div>
        <div class="metric"><span>Investments</span><span style="color:#0284c7">₹90,000</span></div>
        <div class="metric"><span>Monthly spend</span><span style="color:#d97706">₹35,000</span></div>
        <div class="metric"><span>Financial score</span><span>87 / 100</span></div>
        <div class="bar" style="margin-top:16px"><i style="--w:87%"></i></div>
      </div>
    </section>

    <section class="section reveal show">
      <div class="section-head">
        <h2>Built for everyday money decisions</h2>
        <p>Practical tools with charts, filters, downloads, and guided support.</p>
      </div>
      <div class="grid">
        <article class="card"><div class="ico c1"><i class="fas fa-receipt"></i></div><h3>Transactions</h3><p>Log income and expenses with search, filters, and pagination.</p></article>
        <article class="card"><div class="ico c2"><i class="fas fa-piggy-bank"></i></div><h3>Savings vault</h3><p>Watch balances grow with colorful progress and history charts.</p></article>
        <article class="card"><div class="ico c3"><i class="fas fa-chart-pie"></i></div><h3>Budgets</h3><p>Set category limits and spot overspending before it becomes a problem.</p></article>
        <article class="card"><div class="ico c4"><i class="fas fa-bullseye"></i></div><h3>Goals</h3><p>Plan targets and track completion with animated progress bars.</p></article>
        <article class="card"><div class="ico c5"><i class="fas fa-file-arrow-down"></i></div><h3>Downloadable reports</h3><p>Generate summaries and export TXT or CSV in one click.</p></article>
        <article class="card"><div class="ico c6"><i class="fas fa-headset"></i></div><h3>Help desk bot</h3><p>Menu-based guidance for navigation, savings, and admin roles.</p></article>
      </div>
    </section>

    <section class="stats reveal show">
      <div><strong>10,000+</strong><span>Active workspaces</span></div>
      <div><strong>50+</strong><span>Cr managed (demo)</span></div>
      <div><strong>99%</strong><span>Uptime focus</span></div>
      <div><strong>24/7</strong><span>In-app support</span></div>
    </section>

    <section class="cta reveal show">
      <h2>Ready to organize your finances?</h2>
      <p>Create an account, complete setup, and explore a fully working workspace with charts and reports.</p>
      <a class="btn btn-solid" routerLink="/register"><i class="fas fa-user-plus"></i> Create free account</a>
    </section>
    <footer>© 2026 MicroVault · Business personal finance workspace</footer>
  `,
})
export class LandingComponent {
  readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 40);
  }

  tilt(event: MouseEvent): void {
    const card = event.currentTarget as HTMLElement;
    const rect = card.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    card.style.transform = `perspective(1000px) rotateX(${(rect.height / 2 - y) / 18}deg) rotateY(${(x - rect.width / 2) / 18}deg)`;
  }

  resetTilt(event: MouseEvent): void {
    (event.currentTarget as HTMLElement).style.transform = '';
  }
}
