import { NgClass } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { ApiError } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ROLES } from '../core/format';
import { VaultService } from '../core/vault.service';

interface NavLink {
  kind: 'link';
  path: string;
  label: string;
  icon: string;
  color: string;
  index: number;
}

interface NavSection {
  kind: 'section';
  title: string;
}

type NavItem = NavLink | NavSection;

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgClass],
  template: `
    <div class="dashboard">
      <aside class="sidebar" [class.role-admin]="role() === 'admin'" [class.role-super]="role() === 'superadmin'" [class.role-member]="role() === 'user'">
        <div class="sidebar-brand">
          <div class="brand-mark"><i class="fas fa-vault"></i></div>
          <div>
            <strong>MicroVault</strong>
            <small class="role-chip">{{ roleLabel() }}</small>
          </div>
          <button type="button" class="sidebar-close" aria-label="Close menu" (click)="closeMenu()"><i class="fas fa-times"></i></button>
        </div>
        <div class="sidebar-user">
          <div class="avatar">{{ initial() }}</div>
          <div>
            <strong>{{ user()?.fullName || 'User' }}</strong>
            <small>{{ user()?.email }}</small>
          </div>
        </div>
        <nav>
          <ul class="sidebar-nav">
            @for (item of nav(); track item.kind === 'link' ? item.path : item.title) {
              @if (item.kind === 'section') {
                @if (item.title) {
                  <li class="nav-section">{{ item.title }}</li>
                } @else {
                  <li class="nav-divider" aria-hidden="true"></li>
                }
              } @else {
                <li class="nav-item" [ngClass]="item.color" routerLinkActive="active" [style.--nav-i]="item.index + 's'">
                  <a [routerLink]="item.path" (click)="closeMenu()">
                    <span class="nav-ico"><i [class]="'fas fa-' + item.icon"></i></span>
                    <span>{{ item.label }}</span>
                  </a>
                </li>
              }
            }
          </ul>
        </nav>
      </aside>
      <div class="sidebar-overlay" (click)="closeMenu()"></div>
      <div class="main-content">
        <div class="topbar">
          <button type="button" class="menu-toggle" aria-label="Open menu" (click)="openMenu()"><i class="fas fa-bars"></i></button>
          <div class="topbar-title">{{ title() }}</div>
          <div class="topbar-date"><i class="fas fa-calendar-day"></i> <span>{{ dateLabel }}</span></div>
        </div>
        @if (vault.loading() && !vault.loaded()) {
          <div class="empty-state">Loading your workspace…</div>
        } @else if (vault.error() && !vault.loaded()) {
          <div class="alert-box danger">{{ vault.error() }}</div>
        } @else {
          <router-outlet />
        }
      </div>
    </div>
  `,
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly vault = inject(VaultService);

  readonly user = this.auth.user;
  readonly title = signal('Dashboard');
  readonly dateLabel = new Date().toLocaleDateString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
  });

  readonly role = computed(() => this.user()?.role || ROLES.USER);
  readonly roleLabel = computed(() => {
    if (this.role() === ROLES.SUPERADMIN) return 'Super Admin';
    if (this.role() === ROLES.ADMIN) return 'Admin';
    return 'Member';
  });
  readonly initial = computed(() => (this.user()?.fullName || 'U').charAt(0).toUpperCase());
  readonly nav = computed(() => buildNav(this.role()));

  ngOnInit(): void {
    this.syncTitle();
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      this.syncTitle();
      this.closeMenu();
    });
    if (!this.vault.loaded()) {
      this.vault.load().catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 401) {
          void this.router.navigateByUrl('/login');
        }
      });
    }
  }

  openMenu(): void {
    document.body.classList.add('sidebar-open');
  }

  closeMenu(): void {
    document.body.classList.remove('sidebar-open');
  }

  private syncTitle(): void {
    let current = this.route;
    while (current.firstChild) current = current.firstChild;
    this.title.set(String(current.snapshot.data['title'] || 'Dashboard'));
  }
}

function link(path: string, label: string, icon: string, color: string, index: number): NavLink {
  return { kind: 'link', path, label, icon, color, index };
}

function buildNav(role: string): NavItem[] {
  if (role === ROLES.SUPERADMIN) {
    return [
      { kind: 'section', title: 'Admin' },
      link('/admin', 'Dashboard', 'gauge-high', 'nav-blue', 0),
      link('/admin/users', 'Manage Users', 'users', 'nav-violet', 1),
      link('/admin/feedback', 'Feedback', 'comments', 'nav-fuchsia', 2),
      link('/admin/news', 'News', 'newspaper', 'nav-amber', 3),
      { kind: 'section', title: 'Super Admin' },
      link('/admin/admins', 'Manage Admins', 'user-shield', 'nav-gold', 4),
      { kind: 'section', title: 'Account' },
      link('/settings', 'Settings', 'gear', 'nav-slate', 5),
      link('/help', 'Help & Support', 'headset', 'nav-lime', 6),
      { kind: 'section', title: '' },
      link('/logout', 'Logout', 'right-from-bracket', 'nav-red', 7),
    ];
  }
  if (role === ROLES.ADMIN) {
    return [
      { kind: 'section', title: 'Admin' },
      link('/admin', 'Dashboard', 'gauge-high', 'nav-blue', 0),
      link('/admin/users', 'Manage Users', 'users', 'nav-violet', 1),
      link('/admin/feedback', 'Feedback', 'comments', 'nav-fuchsia', 2),
      link('/admin/news', 'News', 'newspaper', 'nav-amber', 3),
      { kind: 'section', title: 'Account' },
      link('/settings', 'Settings', 'gear', 'nav-slate', 4),
      link('/help', 'Help & Support', 'headset', 'nav-lime', 5),
      { kind: 'section', title: '' },
      link('/logout', 'Logout', 'right-from-bracket', 'nav-red', 6),
    ];
  }
  return [
    { kind: 'section', title: 'Main' },
    link('/dashboard', 'Dashboard', 'gauge-high', 'nav-teal', 0),
    link('/goals', 'Goals', 'bullseye', 'nav-amber', 1),
    link('/savings', 'Savings', 'piggy-bank', 'nav-green', 2),
    link('/transactions', 'Transactions', 'arrow-right-arrow-left', 'nav-sky', 3),
    link('/budget', 'Budget', 'chart-pie', 'nav-orange', 4),
    link('/affordability', 'Affordability', 'scale-balanced', 'nav-cyan', 5),
    link('/notifications', 'Notifications', 'bell', 'nav-rose', 6),
    link('/news', 'News', 'newspaper', 'nav-cyan', 7),
    link('/reports', 'Reports', 'file-lines', 'nav-indigo', 8),
    { kind: 'section', title: 'Support' },
    link('/feedback', 'Feedback', 'comment-dots', 'nav-pink', 9),
    link('/help', 'Help & Support', 'headset', 'nav-lime', 10),
    link('/settings', 'Settings', 'gear', 'nav-slate', 11),
    { kind: 'section', title: '' },
    link('/logout', 'Logout', 'right-from-bracket', 'nav-red', 12),
  ];
}
