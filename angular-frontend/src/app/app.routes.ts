import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell.component';
import { adminGuard, authGuard, guestGuard, memberGuard, superGuard } from './core/guards';
import { AdminAdminsComponent } from './pages/admin-admins.component';
import { AdminFeedbackComponent } from './pages/admin-feedback.component';
import { AdminNewsComponent } from './pages/admin-news.component';
import { AdminOverviewComponent } from './pages/admin-overview.component';
import { AdminUsersComponent } from './pages/admin-users.component';
import { AffordabilityComponent } from './pages/affordability.component';
import { BudgetComponent } from './pages/budget.component';
import { DashboardComponent } from './pages/dashboard.component';
import { FeedbackComponent } from './pages/feedback.component';
import { ForgotComponent } from './pages/forgot.component';
import { GoalsComponent } from './pages/goals.component';
import { HelpComponent } from './pages/help.component';
import { LandingComponent } from './pages/landing.component';
import { LoginComponent } from './pages/login.component';
import { LogoutComponent } from './pages/logout.component';
import { NewsComponent } from './pages/news.component';
import { NotificationsComponent } from './pages/notifications.component';
import { RegisterComponent } from './pages/register.component';
import { ReportsComponent } from './pages/reports.component';
import { SavingsComponent } from './pages/savings.component';
import { SettingsComponent } from './pages/settings.component';
import { SetupComponent } from './pages/setup.component';
import { TermsComponent } from './pages/terms.component';
import { TransactionsComponent } from './pages/transactions.component';

export const routes: Routes = [
  { path: '', component: LandingComponent, canActivate: [guestGuard], pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotComponent, canActivate: [guestGuard] },
  { path: 'terms', component: TermsComponent },
  { path: 'setup', component: SetupComponent, canActivate: [authGuard, memberGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [memberGuard], data: { title: 'Dashboard' } },
      { path: 'goals', component: GoalsComponent, canActivate: [memberGuard], data: { title: 'Goals' } },
      { path: 'savings', component: SavingsComponent, canActivate: [memberGuard], data: { title: 'Savings' } },
      { path: 'transactions', component: TransactionsComponent, canActivate: [memberGuard], data: { title: 'Transactions' } },
      { path: 'budget', component: BudgetComponent, canActivate: [memberGuard], data: { title: 'Budget' } },
      { path: 'affordability', component: AffordabilityComponent, canActivate: [memberGuard], data: { title: 'Affordability' } },
      { path: 'notifications', component: NotificationsComponent, canActivate: [memberGuard], data: { title: 'Notifications' } },
      { path: 'news', component: NewsComponent, canActivate: [memberGuard], data: { title: 'News' } },
      { path: 'reports', component: ReportsComponent, canActivate: [memberGuard], data: { title: 'Reports' } },
      { path: 'feedback', component: FeedbackComponent, canActivate: [memberGuard], data: { title: 'Feedback' } },
      { path: 'help', component: HelpComponent, data: { title: 'Help & Support' } },
      { path: 'settings', component: SettingsComponent, data: { title: 'Settings' } },
      { path: 'logout', component: LogoutComponent, data: { title: 'Logout' } },
      { path: 'admin', component: AdminOverviewComponent, canActivate: [adminGuard], data: { title: 'Dashboard' } },
      { path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard], data: { title: 'Manage Users' } },
      { path: 'admin/feedback', component: AdminFeedbackComponent, canActivate: [adminGuard], data: { title: 'Feedback' } },
      { path: 'admin/news', component: AdminNewsComponent, canActivate: [adminGuard], data: { title: 'News' } },
      { path: 'admin/admins', component: AdminAdminsComponent, canActivate: [superGuard], data: { title: 'Manage Admins' } },
    ],
  },
  { path: '**', redirectTo: '' },
];
