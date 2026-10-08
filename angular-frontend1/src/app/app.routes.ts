import { Routes } from '@angular/router';
import { ForgotComponent } from './component/Auth/forgot/forgot.component';
import { LandingComponent } from './component/Auth/landing/landing.component';
import { LoginComponent } from './component/Auth/login.component';
import { LogoutComponent } from './component/Auth/logout/logout.component';
import { RegisterComponent } from './component/Auth/register/register.component';
import { TermsComponent } from './component/Auth/terms/terms.component';
import { AdminAdminsComponent } from './component/Admin/admin-admins/admin-admins.component';
import { AdminFeedbackComponent } from './component/Admin/admin-feedback/admin-feedback.component';
import { AdminNewsComponent } from './component/Admin/admin-news/admin-news.component';
import { AdminOverviewComponent } from './component/Admin/admin-overview.component';
import { AdminUsersComponent } from './component/Admin/admin-users/admin-users.component';
import { AffordabilityComponent } from './component/Finance/affordability/affordability.component';
import { BudgetComponent } from './component/Finance/budget/budget.component';
import { DashboardComponent } from './component/Finance/dashboard.component';
import { GoalsComponent } from './component/Finance/goals/goals.component';
import { ReportsComponent } from './component/Finance/reports/reports.component';
import { SavingsComponent } from './component/Finance/savings/savings.component';
import { TransactionsComponent } from './component/Finance/transactions/transactions.component';
import { FeedbackComponent } from './component/User/feedback/feedback.component';
import { HelpComponent } from './component/User/help/help.component';
import { NewsComponent } from './component/User/news/news.component';
import { NotificationsComponent } from './component/User/notifications/notifications.component';
import { SettingsComponent } from './component/User/settings.component';
import { SetupComponent } from './component/User/setup/setup.component';
import { adminGuard, authGuard, guestGuard, memberGuard, superGuard } from './service/guards';
import { ShellComponent } from './shared/shell/shell.component';

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
