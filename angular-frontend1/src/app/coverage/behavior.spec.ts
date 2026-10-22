import { HttpErrorResponse, HttpEventType, HttpRequest, HttpResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { ActivatedRoute, NavigationEnd, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AppComponent } from '../app.component';
import { routes } from '../app.routes';
import { AdminAdminsComponent } from '../component/Admin/admin-admins/admin-admins.component';
import { AdminApprovalsComponent } from '../component/Admin/admin-approvals/admin-approvals.component';
import { AdminFeedbackComponent } from '../component/Admin/admin-feedback/admin-feedback.component';
import { AdminNewsComponent } from '../component/Admin/admin-news/admin-news.component';
import { AdminOverviewComponent } from '../component/Admin/admin-overview.component';
import { AdminUsersComponent } from '../component/Admin/admin-users/admin-users.component';
import { ForgotComponent } from '../component/Auth/forgot/forgot.component';
import { LandingComponent } from '../component/Auth/landing/landing.component';
import { LoginComponent } from '../component/Auth/login.component';
import { LogoutComponent } from '../component/Auth/logout/logout.component';
import { PrivacyComponent } from '../component/Auth/privacy/privacy.component';
import { RegisterComponent } from '../component/Auth/register/register.component';
import { TermsDialogComponent } from '../component/Auth/terms/terms-dialog.component';
import { TermsComponent } from '../component/Auth/terms/terms.component';
import { AffordabilityComponent } from '../component/Finance/affordability/affordability.component';
import { BudgetComponent } from '../component/Finance/budget/budget.component';
import { DashboardComponent } from '../component/Finance/dashboard.component';
import { GoalsComponent } from '../component/Finance/goals/goals.component';
import { ReportsComponent } from '../component/Finance/reports/reports.component';
import { SavingsComponent } from '../component/Finance/savings/savings.component';
import { TransactionsComponent } from '../component/Finance/transactions/transactions.component';
import { FeedbackComponent } from '../component/User/feedback/feedback.component';
import { HelpComponent } from '../component/User/help/help.component';
import { NewsComponent } from '../component/User/news/news.component';
import { NotificationsComponent } from '../component/User/notifications/notifications.component';
import { SettingsComponent } from '../component/User/settings.component';
import { SetupComponent } from '../component/User/setup/setup.component';
import { environment } from '../../environments/environment';
import { AdminService } from '../service/admin.service';
import { ApiService } from '../service/api.service';
import { authInterceptor } from '../service/auth.interceptor';
import { AuthService, VaultUser } from '../service/auth.service';
import { FinanceService } from '../service/finance.service';
import { adminGuard, authGuard, guestGuard, memberGuard, superGuard } from '../service/guards';
import { LOGIN_NOTICE_KEY, sessionInterceptor } from '../service/session.interceptor';
import { successInterceptor } from '../service/success.interceptor';
import { UserService } from '../service/user.service';
import { WorkspaceService } from '../service/workspace.service';
import { ApiError } from '../shared/api-error';
import { clearCharts, destroyChart, mountChart } from '../shared/charts';
import { DialogHostComponent } from '../shared/dialog-host.component';
import { DialogService } from '../shared/dialog.service';
import { GraphPagerComponent } from '../shared/graph-pager.component';
import { ShellComponent } from '../shared/shell/shell.component';
import { PagerComponent } from '../shared/pager.component';
import { SiteFooterComponent } from '../shared/site-footer.component';
import { ToastHostComponent } from '../shared/toast-host.component';
import { ToastService } from '../shared/toast.service';
import { monthExpenseTotal } from '../shared/finance-rules';
import { Feedback, Goal, Saving } from '../model/models';

const API = environment.apiBase;

describe('application behavior', () => {
  beforeEach(() => boot());

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });

  it('reads the api base used by the client', () => expect(API).toContain('/api'));

  it('rejects a login the server does not accept', async () => {
    const auth = TestBed.inject(AuthService);
    const pending = auth.login('a@b.co', 'x');
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: false, message: 'Invalid email or password' }));
    expect(await pending).toEqual(jasmine.objectContaining({ ok: false }));
    expect(auth.loggedIn()).toBeFalse();
  });

  it('rejects a login that does not return a token', async () => {
    const pending = TestBed.inject(AuthService).login('a@b.co', 'x');
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: true, token: 'only-token' }));
    expect((await pending).ok).toBeFalse();
  });

  it('turns a login transport error into a form error', async () => {
    const pending = TestBed.inject(AuthService).login('a@b.co', 'x');
    http().expectOne(`${API}/auth/login`).flush('{"message":"Invalid email or password","errors":{"email":"Unknown email"}}', { status: 401, statusText: 'Unauthorized' });
    const result = await pending;
    expect(result.ok).toBeFalse();
    expect(result.message).toBe('Invalid email or password');
    expect(result.message).toBe('Invalid email or password');
    expect(result.fieldErrors?.['email']).toBe('Invalid email or password');
    spyOn(TestBed.inject(ApiService), 'raw').and.rejectWith('offline');
    expect((await TestBed.inject(AuthService).login('a@b.co', 'x')).message).toBe('We could not sign you in. Please try again.');
  });

  it('stores a successful login and ignores a broken saved user', async () => {
    const pending = TestBed.inject(AuthService).login('a@b.co', 'Str0ng@1');
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({
      ok: true, token: 'tok', user: { id: '1', fullName: 'Ada Lovelace', email: 'a@b.co', role: 'user', phone: '9876543210' },
    }));
    expect((await pending).ok).toBeTrue();
    const auth = TestBed.inject(AuthService);
    expect(auth.loggedIn()).toBeTrue();
    auth.patchUser({ fullName: 'Ada B Lovelace' });
    expect(JSON.parse(localStorage.getItem('mv_currentUser') || '{}').fullName).toBe('Ada B Lovelace');
    auth.patchUser({});
    expect(auth.user()?.id).toBe('1');
    const saved = auth.updateProfile({ fullName: 'Ada Lovelace', email: 'a@b.co', phone: '9876543210' });
    http().expectOne(`${API}/users/me`).flush(JSON.stringify({ data: { id: '1', fullName: 'Ada Lovelace', email: 'a@b.co', role: 'user' } }));
    expect((await saved).fullName).toBe('Ada Lovelace');
    const password = auth.changePassword({ currentPassword: 'a', newPassword: 'b', confirmPassword: 'b' });
    http().expectOne(`${API}/users/me/password`).flush('');
    await password;
    const otp = auth.sendResetOtp('a@b.co');
    http().expectOne(`${API}/auth/forgot/send-otp`).flush(JSON.stringify({ data: { demoOtp: '123456' } }));
    expect((await otp).demoOtp).toBe('123456');
    const reset = auth.resetPassword({ email: 'a@b.co', otp: '123456', password: 'b', confirmPassword: 'b' });
    http().expectOne(`${API}/auth/forgot/reset`).flush(JSON.stringify({ data: { ok: true } }));
    await reset;
    const logout = auth.logout();
    http().expectOne(`${API}/auth/logout`).error(new ProgressEvent('error'));
    await logout;
    expect(auth.loggedIn()).toBeFalse();
    auth.patchUser({ fullName: 'nobody' });
    expect(auth.user()).toBeNull();
  });

  it('registers a member and reports a rejected registration', async () => {
    const auth = TestBed.inject(AuthService);
    const pending = auth.register(person());
    http().expectOne(`${API}/auth/register`).flush(JSON.stringify({ data: member() }));
    expect((await pending).ok).toBeTrue();
    const failed = auth.register(person());
    http().expectOne(`${API}/auth/register`).flush('{"message":"Email already registered"}', { status: 409, statusText: 'Conflict' });
    expect((await failed).ok).toBeFalse();
    spyOn(TestBed.inject(ApiService), 'post').and.rejectWith('offline');
    expect((await auth.register(person())).message).toContain('could not complete');
  });

  it('ignores a corrupt saved session', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor, sessionInterceptor, successInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    localStorage.setItem('mv_currentUser', '{');
    expect(TestBed.inject(AuthService).user()).toBeNull();
  });

  it('applies workspace payloads, drops a stale refresh, and classifies budgets', async () => {
    resign(member());
    const workspace = TestBed.inject(WorkspaceService);
    const auth = TestBed.inject(AuthService);
    expect(workspace.needsSetup()).toBeTrue();
    const first = workspace.refresh();
    const second = workspace.refresh();
    const requests = http().match(`${API}/dashboard`);
    expect(requests.length).toBe(2);
    requests[1].flush(JSON.stringify({ data: payload() }));
    requests[0].flush(JSON.stringify({ data: { user: { id: 'old', fullName: 'Old Name', email: 'old@b.co', role: 'user' } } }));
    await Promise.all([first, second]);
    expect(auth.user()?.fullName).toBe('Ada Lovelace');
    expect(workspace.transactions().length).toBe(1);
    expect(workspace.budgets().map((row) => row.status)).toEqual(['over', 'warn', 'warn', 'over', 'warn', 'ok', 'ok']);
    expect(workspace.savingsOverview()?.protectedForNeeds).toBe(30);
    expect(workspace.allFeedback().length).toBe(1);
    expect(workspace.allNews().length).toBe(1);
    expect(workspace.insights().recentMembers.length).toBe(1);
    expect(workspace.loaded()).toBeTrue();
    const sparseMoney = workspace.refresh();
    http().expectOne(`${API}/dashboard`).flush(JSON.stringify({ data: {
      savingsOverview: { allocatedSavings: 1, unallocatedLiquid: 1, emergencyReserve: 1, totalSavings: 3, investments: 1, protectedForNeeds: null, availableForWants: null },
      feedback: [{ id: 'f', subject: 'S', message: 'M', status: 'open' }],
      allFeedback: [{ id: 'f2', subject: 'All', message: 'M', status: 'resolved' }],
      news: [{ id: 'n', title: 'T', body: 'B' }],
      allNews: [{ id: 'n2', title: 'All news', body: 'B' }],
      notifications: [
        { id: 'n1', title: 'Hi', message: 'There', type: 'info', read: false },
        { id: 'n2', title: 'Ho', message: 'There', type: 'info', read: false },
      ],
    } }));
    await sparseMoney;
    expect(workspace.savingsOverview()?.protectedForNeeds).toBeNull();
    expect(workspace.savingsOverview()?.availableForWants).toBeNull();
    expect(workspace.allFeedback()[0].id).toBe('f2');
    expect(workspace.allNews()[0].id).toBe('n2');
    workspace.markReadLocally('n1');
    expect(workspace.notifications().find((row) => row.id === 'n2')?.read).toBeFalse();
    const notes = workspace.refreshNotifications();
    http().expectOne(`${API}/notifications`).flush(JSON.stringify({ data: { not: 'a list' } }));
    await notes;
    expect(workspace.notifications()).toEqual([]);
    workspace.markReadLocally('n1');
    workspace.markReadLocally();
    workspace.markDependentsStale();
    expect(workspace.affordabilityStale()).toBeTrue();
    workspace.acknowledgeAffordability();
    workspace.acknowledgePlan();
    expect(workspace.planStale()).toBeFalse();
  });

  it('uses fallback collections and clears the session after an unauthorized load', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    const sparse = workspace.refresh();
    http().expectOne(`${API}/dashboard`).flush(JSON.stringify({
      data: { setupSkipped: true, feedback: [{ id: 'f', subject: 'S', message: 'M', status: 'open' }], news: [{ id: 'n', title: 'T', body: 'B' }] },
    }));
    await sparse;
    expect(workspace.setupSkipped()).toBeTrue();
    expect(workspace.allFeedback().length).toBe(1);
    expect(workspace.allNews().length).toBe(1);
    expect(workspace.stats()).toBeNull();
    expect(workspace.savingsOverview()).toBeNull();
    const failed = TestBed.inject(WorkspaceService).load();
    http().expectOne(`${API}/dashboard`).flush('{"message":"Login is required"}', { status: 401, statusText: 'Unauthorized' });
    await expectAsync(failed).toBeRejected();
    expect(TestBed.inject(AuthService).token()).toBe('');
    spyOn(TestBed.inject(ApiService), 'get').and.rejectWith('down');
    await expectAsync(workspace.load()).toBeRejected();
    expect(workspace.error()).toBe('Could not load workspace');
  });

  it('does not ask an administrator to finish financial setup', () => {
    resign(member('admin'));
    const workspace = TestBed.inject(WorkspaceService);
    expect(workspace.needsSetup()).toBeFalse();
    resign(member('superadmin'));
    expect(TestBed.inject(WorkspaceService).needsSetup()).toBeFalse();
  });

  it('writes finance records and keeps a warning when the refresh fails', async () => {
    const finance = TestBed.inject(FinanceService);
    const workspace = TestBed.inject(WorkspaceService);
    workspace.finance.set({ monthlyIncome: 1000, additionalIncome: 100 });
    workspace.budgets.set([{ id: 'b', category: 'Food', limit: 200, spent: 10 }, { id: 'z', category: 'Misc', limit: 0 }]);
    workspace.goals.set([goal(), { ...goal(), id: 'draft', status: 'draft', plannedContribution: 999 }, { ...goal(), id: 'zero-plan', status: 'active', plannedContribution: 0 }, { ...goal(), id: 'nostatus', status: '' }]);
    expect(finance.capacity()?.income).toBe(1100);
    expect(finance.position().monthlyBudget).toBe(200);
    workspace.finance.set({ monthlyIncome: 0 });
    expect(finance.capacity()).toBeNull();
    expect(finance.needsSetup()).toBeFalse();
    finance.acknowledgePlan();

    const saved = finance.saveFinance({ monthlyIncome: 1000 });
    http().expectOne(`${API}/finance`).flush(JSON.stringify({ data: { monthlyIncome: 1000 } }));
    await flushDash();
    expect((await saved).monthlyIncome).toBe(1000);

    const skipped = finance.skipSetup();
    const skip = http().expectOne(`${API}/finance/skip`);
    expect(skip.request.body).toEqual({});
    skip.flush('');
    await Promise.resolve();
    await flushDash();
    await skipped;

    const added = finance.addTx({ name: 'Rent' });
    http().expectOne(`${API}/transactions`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    expect(await added).toContain('down');

    await expectWrite(() => finance.updateTx('1', {}), `${API}/transactions/1`, 'PUT');
    await expectWrite(() => finance.addGoal({}), `${API}/goals`, 'POST');
    await expectWrite(() => finance.updateGoal('1', {}), `${API}/goals/1`, 'PUT');
    await expectWrite(() => finance.deleteGoal('1'), `${API}/goals/1`, 'DELETE');
    await expectWrite(() => finance.clearGoals(), `${API}/goals`, 'DELETE');
    await expectWrite(() => finance.addSaving({}), `${API}/savings`, 'POST');
    await expectWrite(() => finance.updateSaving('1', {}), `${API}/savings/1`, 'PUT');
    await expectWrite(() => finance.deleteSaving('1'), `${API}/savings/1`, 'DELETE');
    await expectWrite(() => finance.clearSavings(), `${API}/savings`, 'DELETE');
    await expectWrite(() => finance.addBudget({}), `${API}/budgets`, 'POST');
    await expectWrite(() => finance.updateBudget('1', {}), `${API}/budgets/1`, 'PUT');
    await expectWrite(() => finance.deleteBudget('1'), `${API}/budgets/1`, 'DELETE');
    await expectWrite(() => finance.clearBudgets(), `${API}/budgets`, 'DELETE');

    const report = finance.generateReport({ fromDate: '2026-01-01', toDate: '2026-01-31', type: 'All' });
    http().expectOne(`${API}/reports/generate`).flush(JSON.stringify({ data: { id: 'r', type: 'All', fromDate: '2026-01-01', toDate: '2026-01-31', income: 1, expense: 1, net: 0, txCount: 1 } }));
    await flushDash();
    expect((await report).id).toBe('r');

    workspace.affordabilityStale.set(true);
    const check = finance.checkAfford({ itemName: 'Bike', amount: 10 });
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c', itemName: 'Bike', amount: 10, verdict: 'Yes', level: 'success' } }));
    await Promise.resolve();
    await flushDash();
    expect((await check).itemName).toBe('Bike');
    expect(workspace.affordabilityStale()).toBeFalse();

    const summary = finance.monthlySummary('1');
    http().expectOne(`${API}/finance/profile/1/summary`).flush(JSON.stringify({ data: { insights: ['ok'] } }));
    expect((await summary)['insights']).toEqual(['ok']);

    const deletion = finance.requestTransactionDeletion('tx', 'duplicate');
    http().expectOne(`${API}/admin/approvals`).flush(JSON.stringify({ data: { id: 'a', status: 'pending', requestType: 'transaction-delete', resourceId: 'tx', requesterId: '1' } }));
    expect((await deletion).status).toBe('pending');
    const mine = finance.getMyApprovalRequests();
    http().expectOne(`${API}/admin/approvals/mine`).flush(JSON.stringify({ data: [] }));
    expect(await mine).toEqual([]);

    const removed = finance.deleteAfford('c');
    http().expectOne(`${API}/affordability/c`).flush('');
    await flushDash();
    await removed;
    const cleared = finance.clearAfford();
    http().expectOne(`${API}/affordability`).flush('');
    await Promise.resolve();
    await flushDash();
    await cleared;
    const failedDelete = finance.deleteAfford('missing');
    http().expectOne(`${API}/affordability/missing`).flush('');
    (await whenRequested(`${API}/dashboard`)).flush('no', { status: 500, statusText: 'Error' });
    await expectAsync(failedDelete).toBeRejected();
    expect(finance.loaded()).toBeTrue();
  });

  it('creates and maintains members, admins, news, and feedback', async () => {
    const admin = TestBed.inject(AdminService);
    const created = admin.saveMember(null, { fullName: 'Ada Lovelace', email: 'A@B.co', phone: '9876543210', password: 'Str0ng@1', status: 'active' });
    http().expectOne(`${API}/auth/register`).flush(JSON.stringify({ data: member() }));
    (await whenRequested(`${API}/admin/users/1`)).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await created;

    const registerSpy = spyOn(TestBed.inject(AuthService), 'register').and.resolveTo({ ok: false, message: 'Email already registered' });
    await expectAsync(admin.saveMember(null, {})).toBeRejectedWithError('Email already registered');
    registerSpy.and.resolveTo({ ok: false });
    await expectAsync(admin.saveMember(null, {})).toBeRejectedWithError('Could not create user');
    registerSpy.and.resolveTo({ ok: true, user: member() });
    const bare = admin.saveMember(null, { fullName: 'Ada Lovelace', email: 'ada@b.co', phone: '9876543210' });
    const createdUser = await whenRequested(`${API}/admin/users/1`);
    expect(createdUser.request.body.status).toBe('active');
    createdUser.flush(JSON.stringify({ data: {} }));
    await flushDash();
    await bare;

    const updated = admin.saveMember('1', { fullName: 'Ada Lovelace', email: 'a@b.co', phone: '9876543210', status: 'inactive' });
    http().expectOne(`${API}/admin/users/1`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await updated;
    const removed = admin.deleteMember('1');
    http().expectOne(`${API}/admin/users/1`).flush('');
    await flushDash();
    await removed;
    const newAdmin = admin.saveAdmin(null, { fullName: 'Grace Hopper', email: 'g@b.co', phone: '9876543211' });
    http().expectOne(`${API}/admin/admins`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await newAdmin;
    const editedAdmin = admin.saveAdmin('2', { fullName: 'Grace Hopper' });
    http().expectOne(`${API}/admin/admins/2`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await editedAdmin;
    const deletedAdmin = admin.deleteAdmin('2');
    http().expectOne(`${API}/admin/admins/2`).flush('');
    await flushDash();
    await deletedAdmin;
    const news = admin.addNews({ title: 'Hello', body: 'World' });
    http().expectOne(`${API}/admin/news`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await news;
    const edited = admin.updateNews('n', { title: 'Hello' });
    http().expectOne(`${API}/admin/news/n`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await edited;
    const deleted = admin.deleteNews('n');
    http().expectOne(`${API}/admin/news/n`).flush('');
    await flushDash();
    await deleted;
    const feedback = admin.updateFeedback('f', { status: 'resolved' });
    http().expectOne(`${API}/feedback/f`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await feedback;
    const history = admin.history('f');
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: [{ action: 'opened' }] }));
    expect((await history)[0].action).toBe('opened');
    expect(admin.members()).toEqual([]);
    expect(admin.insights().totalMembers).toBe(0);
  });

  it('marks notifications read even when the server call fails', async () => {
    const user = TestBed.inject(UserService);
    const workspace = TestBed.inject(WorkspaceService);
    workspace.notifications.set([{ id: '1', title: 'Hi', message: 'There', type: 'info', read: false }]);
    expect(user.unreadCount()).toBe(1);
    const marked = user.markRead('1');
    http().expectOne(`${API}/notifications/1/read`).flush('no', { status: 500, statusText: 'Error' });
    (await whenRequested(`${API}/notifications`)).flush('no', { status: 500, statusText: 'Error' });
    await expectAsync(marked).toBeRejected();
    expect(workspace.notifications()[0].read).toBeTrue();
    const refreshed = user.refreshNotifications();
    http().expectOne(`${API}/notifications`).flush(JSON.stringify({ data: [] }));
    await refreshed;
    const all = user.markAllRead();
    http().expectOne(`${API}/notifications/read-all`).flush('');
    (await whenRequested(`${API}/notifications`)).flush('no', { status: 500, statusText: 'Error' });
    await all;
    const failedAll = user.markAllRead();
    http().expectOne(`${API}/notifications/read-all`).flush('');
    (await whenRequested(`${API}/notifications`)).flush('no', { status: 500, statusText: 'Error' });
    await failedAll;
    const removed = user.deleteNotice('1');
    http().expectOne(`${API}/notifications/1`).flush('');
    (await whenRequested(`${API}/notifications`)).flush(JSON.stringify({ data: [] }));
    await removed;
    const cleared = user.clearNotices();
    http().expectOne(`${API}/notifications`).flush('');
    (await whenRequested(`${API}/notifications`)).flush(JSON.stringify({ data: [] }));
    await cleared;
    const added = user.addFeedback({ subject: 'S' });
    http().expectOne(`${API}/feedback`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await added;
    const edited = user.updateFeedback('f', { subject: 'S' });
    http().expectOne(`${API}/feedback/f`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await edited;
    const history = user.history('f');
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: { nope: true } }));
    expect(await history).toEqual([]);
    const load = user.load();
    await flushDash();
    await load;
    const retry = user.retryRefresh();
    await flushDash();
    await retry;
  });

  it('sends the bearer token only on protected api calls', () => {
    localStorage.setItem('mv_token', 'tok');
    const seen: string[] = [];
    const next = (req: HttpRequest<unknown>) => {
      seen.push(req.headers.get('Authorization') || '');
      return of(new HttpResponse({ status: 200 }));
    };
    TestBed.runInInjectionContext(() => {
      authInterceptor(new HttpRequest('GET', `${API}/dashboard`), next).subscribe();
      authInterceptor(new HttpRequest('POST', `${API}/auth/login`, {}), next).subscribe();
      authInterceptor(new HttpRequest('POST', `${API}/auth/register`, {}), next).subscribe();
      authInterceptor(new HttpRequest('POST', `${API}/auth/forgot/send-otp`, {}), next).subscribe();
      authInterceptor(new HttpRequest('GET', 'http://localhost/health'), next).subscribe();
    });
    expect(seen).toEqual(['Bearer tok', '', '', '', '']);
    localStorage.removeItem('mv_token');
    TestBed.runInInjectionContext(() => authInterceptor(new HttpRequest('GET', `${API}/dashboard`), next).subscribe());
    expect(seen[5]).toBe('');
  });

  it('ends a rejected session and leaves login failures alone', () => {
    resign(member());
    const auth = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const next = (status: number, url: string, body: unknown) => TestBed.runInInjectionContext(() => sessionInterceptor(
      new HttpRequest('GET', url),
      () => throwError(() => new HttpErrorResponse({ status, url, error: body })),
    ).subscribe({ error: () => undefined }));
    next(401, `${API}/dashboard`, { message: 'Login is required' });
    expect(auth.token()).toBe('');
    expect(sessionStorage.getItem(LOGIN_NOTICE_KEY)).toContain('session has ended');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    resign(member());
    next(401, `${API}/auth/login`, { message: 'Login is required' });
    next(401, `${API}/auth/logout`, { message: 'Login is required' });
    next(403, `${API}/dashboard`, { message: 'Login is required' });
    next(401, `${API}/dashboard`, null);
    next(401, 'http://localhost/health', { message: 'Login is required' });
    expect(TestBed.inject(AuthService).token()).toBe('tok');
    TestBed.runInInjectionContext(() => sessionInterceptor(
      new HttpRequest('GET', `${API}/dashboard`),
      () => of(new HttpResponse({ status: 200 })),
    ).subscribe());
  });

  it('toasts a completed change and ignores reads and failed statuses', () => {
    const toast = TestBed.inject(ToastService);
    const spy = spyOn(toast, 'success');
    const respond = (method: string, url: string, status: number, type: HttpEventType = HttpEventType.Response) => TestBed.runInInjectionContext(() => {
      successInterceptor(new HttpRequest(method, url, null), () => of(type === HttpEventType.Response
        ? new HttpResponse({ status })
        : { type } as never)).subscribe();
    });
    respond('GET', `${API}/dashboard`, 200);
    respond('POST', `${API}/transactions`, 201);
    respond('POST', `${API}/transactions`, 500);
    respond('POST', `${API}/transactions`, 200, HttpEventType.Sent);
    respond('POST', `${API}/finance/skip`, 200);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('guards routes by session and role', () => {
    expect(String(runGuard(authGuard))).toContain('login');
    expect(runGuard(guestGuard)).toBeTrue();
    expect(String(runGuard(memberGuard))).toContain('dashboard');
    resign(member());
    expect(runGuard(authGuard)).toBeTrue();
    expect(String(runGuard(guestGuard))).toContain('dashboard');
    expect(runGuard(memberGuard)).toBeTrue();
    expect(String(runGuard(adminGuard))).toContain('dashboard');
    resign(member('admin'));
    expect(runGuard(adminGuard)).toBeTrue();
    expect(String(runGuard(superGuard))).toContain('admin');
    expect(String(runGuard(memberGuard))).toContain('admin');
    resign(member('superadmin'));
    expect(runGuard(superGuard)).toBeTrue();
    expect(String(runGuard(guestGuard))).toContain('admin');
  });

  it('parses api responses, including an empty body and invalid json', async () => {
    const api = TestBed.inject(ApiService);
    const get = api.get<{ ok: boolean }>('/health');
    http().expectOne(`${API}/health`).flush(JSON.stringify({ ok: true }));
    expect(await get).toEqual({ ok: true });
    const raw = api.raw('GET', '/status');
    http().expectOne(`${API}/status`).flush('   ');
    expect(await raw).toBeUndefined();
    const broken = api.get('/broken');
    http().expectOne(`${API}/broken`).flush('not-json');
    await expectAsync(broken).toBeRejectedWith(jasmine.any(ApiError));
    const deleted = api.delete('/goals/1');
    http().expectOne(`${API}/goals/1`).flush('');
    await deleted;
    const emptyBody = api.get('/empty-body');
    http().expectOne(`${API}/empty-body`).flush(null);
    expect(await emptyBody).toBeUndefined();
    const put = api.put('/finance', { monthlyIncome: 1 });
    http().expectOne(`${API}/finance`).flush(JSON.stringify({ data: { monthlyIncome: 1 } }));
    expect(await put).toEqual({ monthlyIncome: 1 });
  });

  it('mounts, replaces, and clears charts', () => {
    expect(mountChart(null, { type: 'bar', data: { labels: [], datasets: [] } })).toBeNull();
    const canvas = document.createElement('canvas');
    document.body.appendChild(canvas);
    const first = mountChart(canvas, { type: 'bar', data: { labels: ['A'], datasets: [{ data: [1] }] } });
    expect(first).toBeTruthy();
    mountChart(canvas, { type: 'bar', data: { labels: ['B'], datasets: [{ data: [2] }] } });
    destroyChart(null);
    destroyChart(document.createElement('canvas'));
    destroyChart(canvas);
    clearCharts();
    canvas.remove();
  });

  it('confirms, notices, and dismisses dialogs from the host', async () => {
    const dialog = TestBed.inject(DialogService);
    const pending = dialog.confirm('Delete this goal?');
    expect(dialog.state()?.title).toBe('Confirm delete');
    const fixture = TestBed.createComponent(DialogHostComponent);
    fixture.detectChanges();
    fixture.componentInstance.onEscape();
    expect(await pending).toBeFalse();
    fixture.componentInstance.onEscape();
    const info = dialog.notice('Saved');
    fixture.detectChanges();
    const backdrop = document.createElement('div');
    fixture.componentInstance.backdrop({ target: backdrop, currentTarget: backdrop } as unknown as MouseEvent);
    expect(await info).toBeTrue();
    const confirm = dialog.confirm('Delete?', { title: 'Remove', okText: 'Yes', cancelText: 'No', mode: 'delete' });
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.btn-outline').click();
    expect(await confirm).toBeFalse();
    dialog.close(true);
    const infoDefaults = dialog.confirm('Note', { mode: 'info' });
    expect(dialog.state()?.title).toBe('Notice');
    expect(dialog.state()?.okText).toBe('OK');
    expect(dialog.state()?.cancelText).toBe('Cancel');
    const other = document.createElement('div');
    fixture.componentInstance.backdrop({ target: other, currentTarget: backdrop } as unknown as MouseEvent);
    expect(dialog.state()).not.toBeNull();
    dialog.close(true);
    expect(await infoDefaults).toBeTrue();
    fixture.componentInstance.onEscape();
  });

  it('shows and merges toasts', fakeAsync(() => {
    const toast = TestBed.inject(ToastService);
    toast.success('');
    toast.error('');
    expect(toast.toasts()).toEqual([]);
    toast.success('Saved');
    toast.success('Saved again');
    toast.error('Failed');
    toast.success('Third');
    toast.success('Fourth');
    expect(toast.toasts().length).toBeLessThanOrEqual(3);
    const fixture = TestBed.createComponent(ToastHostComponent);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click();
    tick(3800);
    expect(toast.toasts()).toEqual([]);
    toast.dismiss(999);
  }));

  it('pages tables and graphs from the keyboard', () => {
    const pager = TestBed.createComponent(PagerComponent);
    pager.componentRef.setInput('page', 2);
    pager.componentRef.setInput('totalPages', 3);
    pager.componentRef.setInput('total', 12);
    pager.detectChanges();
    const pages: number[] = [];
    pager.componentInstance.pageChange.subscribe((page) => pages.push(page));
    const buttons = pager.nativeElement.querySelectorAll('button');
    buttons[0].click();
    buttons[1].click();
    expect(pages).toEqual([1, 3]);

    const graphs = TestBed.createComponent(GraphPagerComponent);
    graphs.componentRef.setInput('index', 1);
    graphs.componentRef.setInput('total', 3);
    graphs.componentRef.setInput('title', 'Trend');
    graphs.detectChanges();
    const moves: number[] = [];
    graphs.componentInstance.pageChange.subscribe((page) => moves.push(page));
    graphs.componentInstance.previous();
    graphs.componentInstance.next();
    graphs.componentInstance.onKey(key('ArrowLeft'));
    graphs.componentInstance.onKey(key('ArrowRight'));
    graphs.componentInstance.onKey(key('Enter'));
    graphs.componentRef.setInput('index', 0);
    graphs.componentInstance.previous();
    graphs.componentRef.setInput('index', 2);
    graphs.componentInstance.next();
    expect(moves).toEqual([0, 2, 0, 2]);
  });

  it('renders the public pages and the signed-in footer', () => {
    const footer = TestBed.createComponent(SiteFooterComponent);
    footer.detectChanges();
    footer.componentInstance.signedIn = true;
    footer.detectChanges();
    expect(footer.nativeElement.textContent).toContain('Help');
    expect(TestBed.createComponent(PrivacyComponent).nativeElement.textContent).toContain('Privacy');
    expect(TestBed.createComponent(TermsComponent).nativeElement.textContent).toContain('Terms');
    expect(TestBed.createComponent(AppComponent).componentInstance).toBeTruthy();
    const landing = TestBed.createComponent(LandingComponent);
    const card = document.createElement('div');
    spyOn(card, 'getBoundingClientRect').and.returnValue(rect());
    landing.componentInstance.tilt({ currentTarget: card, clientX: 40, clientY: 20 } as unknown as MouseEvent);
    expect(card.style.transform).toContain('rotateX');
    landing.componentInstance.resetTilt({ currentTarget: card } as unknown as MouseEvent);
    expect(card.style.transform).toBe('');
    Object.defineProperty(window, 'scrollY', { value: 80, configurable: true });
    landing.componentInstance.onScroll();
    expect(landing.componentInstance.scrolled()).toBeTrue();
    Object.defineProperty(window, 'scrollY', { value: 0, configurable: true });
    landing.componentInstance.onScroll();
    expect(landing.componentInstance.scrolled()).toBeFalse();
  });

  it('unlocks the terms dialog only after the text is scrolled', fakeAsync(() => {
    const fixture = TestBed.createComponent(TermsDialogComponent);
    fixture.componentInstance.open = true;
    fixture.componentInstance.ngOnChanges({ open: { currentValue: true, previousValue: false, firstChange: true, isFirstChange: () => true } });
    fixture.detectChanges();
    const box = fixture.nativeElement.querySelector('.terms-box') as HTMLElement;
    Object.defineProperty(box, 'scrollTop', { value: 0, configurable: true });
    Object.defineProperty(box, 'clientHeight', { value: 20, configurable: true });
    Object.defineProperty(box, 'scrollHeight', { value: 200, configurable: true });
    fixture.componentInstance.check();
    expect(fixture.componentInstance.read()).toBeFalse();
    Object.defineProperty(box, 'scrollTop', { value: 180, configurable: true });
    fixture.componentInstance.check();
    tick();
    expect(fixture.componentInstance.read()).toBeTrue();
    fixture.componentInstance.accept.subscribe();
    fixture.nativeElement.querySelector('.btn-primary').click();
  }));

  it('answers help questions and falls back to the menu', fakeAsync(() => {
    const fixture = TestBed.createComponent(HelpComponent);
    const help = fixture.componentInstance;
    help.load('missing');
    expect(help.topic()).toBe('missing');
    expect(help.messages()[0].who).toBe('bot');
    help.ask(help.chips()[0]);
    expect(help.messages().length).toBe(2);
    tick(200);
    expect(help.messages().length).toBe(3);
    help.topics.forEach((topic) => help.load(topic.key));
  }));

  it('keeps a blocked member on the login form', async () => {
    sessionStorage.setItem(LOGIN_NOTICE_KEY, 'Your account was blocked');
    const fixture = TestBed.createComponent(LoginComponent);
    const login = fixture.componentInstance;
    expect(login.notice()).toContain('blocked');
    expect(sessionStorage.getItem(LOGIN_NOTICE_KEY)).toBeNull();
    login.onEmail('nope');
    login.onPassword('');
    expect(login.errors()['password']).toContain('required');
    login.onPassword('short');
    await login.submit();
    expect(login.errors()['email']).toContain('valid email');
    login.onEmail('ada@b.co');
    login.onPassword('Str0ng@1');
    const pending = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: false, message: 'Your account has been blocked' }));
    await pending;
    expect(login.blocked()).toBeTrue();
    const blank = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: false, fieldErrors: { email: 'Unknown email' } }));
    await blank;
    expect(login.formError()).toContain('could not sign you in');
    login.show.set(true);
    const success = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: true, token: 'tok', user: member('admin') }));
    await flushDash();
    await success;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalled();
  });

  it('sends a new member to setup when the workspace cannot load', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const login = fixture.componentInstance;
    login.email = 'ada@b.co';
    login.password = 'Str0ng@1';
    const failedLoad = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: true, token: 'tok', user: member() }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 500, statusText: 'Down' });
    await failedLoad;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/setup');
    const skipped = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: true, token: 'tok', user: member() }));
    (await whenRequested(`${API}/dashboard`)).flush(JSON.stringify({ data: { setupSkipped: true } }));
    await skipped;
    const ready = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: true, token: 'tok', user: member() }));
    (await whenRequested(`${API}/dashboard`)).flush(JSON.stringify({ data: { finance: { monthlyIncome: 1 } } }));
    await ready;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('registers only after the terms have been read', async () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    const register = fixture.componentInstance;
    const event = { preventDefault: jasmine.createSpy('prevent'), target: { checked: true } } as unknown as Event;
    register.openTerms(event);
    expect(register.termsOpen()).toBeTrue();
    register.onTermsClick(event);
    expect(event.preventDefault).toHaveBeenCalled();
    await register.submit();
    expect(register.errors()['terms']).toContain('read');
    register.acceptTerms();
    register.onTermsClick({ preventDefault() {}, target: { checked: false } } as unknown as Event);
    expect(register.errors()['terms']).toContain('accept');
    register.acceptTerms();
    register.onFirst('A');
    register.onMiddle('2');
    register.onLast('B');
    register.onEmail('ada@b.co');
    register.onPhone('9876543210');
    register.onPassword('Str0ng@1');
    register.onConfirm('nope');
    await register.submit();
    expect(register.errors()['confirmPassword']).toContain('match');
    register.onTermsClick({ preventDefault() {}, target: { checked: false } } as unknown as Event);
    await register.submit();
    expect(register.errors()['terms']).toContain('accept');
    register.onTermsClick({ preventDefault() {}, target: { checked: true } } as unknown as Event);
    register.onPassword('Str0ng@1');
    register.onConfirm('Str0ng@1');
    await register.submit();
    expect(register.errors()['middleName']).toContain('letters');
    register.onMiddle('');
    register.onFirst('Ada');
    register.onLast('Lovelace');
    const pending = register.submit();
    http().expectOne(`${API}/auth/register`).flush(JSON.stringify({ data: member() }));
    expect((await pending) as unknown).toBeUndefined();
    expect(register.created()).toBeTrue();
    register.onFirst('Ada');
    const failed = register.submit();
    http().expectOne(`${API}/auth/register`).flush('{"message":"Email already registered","errors":{"email":"Email already registered"}}', { status: 409, statusText: 'Conflict' });
    await failed;
    expect(register.created()).toBeFalse();
    expect(register.busy()).toBeFalse();
    spyOn(TestBed.inject(AuthService), 'register').and.resolveTo({ ok: false });
    await register.submit();
    expect(register.formError()).toContain('could not complete');
  });

  it('sends a reset code and stores the new password', async () => {
    const fixture = TestBed.createComponent(ForgotComponent);
    const forgot = fixture.componentInstance;
    expect(forgot.emailOk('')).toContain('required');
    forgot.onPassword('Str0ng@1');
    await forgot.sendCode();
    expect(forgot.errors()['email']).toContain('required');
    forgot.email = 'ada@b.co';
    forgot.touch('email', '');
    const sent = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush(JSON.stringify({ data: { demoOtp: '123456' } }));
    await closeNotice();
    await sent;
    const failedSend = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush('{"message":"No account found","errors":{"email":"No account found"}}', { status: 404, statusText: 'Missing' });
    await failedSend;
    forgot.onOtp('123456');
    forgot.onPassword('Str0ng@1');
    forgot.onConfirm('nope');
    forgot.onPassword('Str0ng@1');
    forgot.onConfirm('Str0ng@1');
    forgot.show.set(true);
    forgot.show2.set(true);
    const reset = forgot.submit();
    http().expectOne(`${API}/auth/forgot/reset`).flush('');
    await closeNotice();
    await reset;
    forgot.otp = '000000';
    const rejected = forgot.submit();
    http().expectOne(`${API}/auth/forgot/reset`).flush('{"message":"Verification code is invalid or expired"}', { status: 400, statusText: 'Bad' });
    await rejected;
    expect(forgot.errors()['otp']).toContain('invalid');
    forgot.otp = '12';
    await forgot.submit();
    expect(forgot.errors()['otp']).toContain('6-digit');
  });

  it('logs out or returns home', async () => {
    resign(member('admin'));
    const fixture = TestBed.createComponent(LogoutComponent);
    fixture.componentInstance.cancel();
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/admin');
    const pending = fixture.componentInstance.confirm();
    http().expectOne(`${API}/auth/logout`).flush('');
    await pending;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('saves a profile and a password from settings', async () => {
    const blank = TestBed.createComponent(SettingsComponent).componentInstance;
    expect(blank.firstName).toBe('');
    expect(blank.email).toBe('');
    expect(blank.phone).toBe('');
    expect(blank.member()).toBeFalse();
    resign(member('admin'));
    expect(TestBed.createComponent(SettingsComponent).componentInstance.member()).toBeFalse();
    resign(member());
    const fixture = TestBed.createComponent(SettingsComponent);
    const settings = fixture.componentInstance;
    expect(settings.member()).toBeTrue();
    settings.onEmail('ada@b.co');
    settings.onPhone('9876543210');
    settings.onName('firstName', 'Ada', 'First name', true);
    settings.onName('middleName', '', 'Middle name', false);
    settings.onName('lastName', 'Lovelace', 'Last name', true);
    const saved = settings.saveProfile();
    http().expectOne(`${API}/users/me`).flush(JSON.stringify({ data: member() }));
    await saved;
    settings.onEmail('nope');
    await settings.saveProfile();
    expect(settings.errors()['email']).toContain('valid email');
    settings.onEmail('ada@b.co');
    const failed = settings.saveProfile();
    http().expectOne(`${API}/users/me`).flush('{"message":"Email already registered","errors":{"fullName":"Full name is required"}}', { status: 409, statusText: 'Conflict' });
    await failed;
    settings.onNewPassword('short');
    expect(settings.passErrors()['confirm'] || '').toBe('');
    settings.onCurrent('');
    settings.onNewPassword('short');
    settings.onConfirm('other');
    settings.onNewPassword('Str0ng@1');
    expect(settings.passErrors()['confirm']).toContain('match');
    await settings.savePassword();
    expect(settings.passErrors()['current']).toContain('required');
    settings.onCurrent('OldPass@1');
    settings.onNewPassword('Str0ng@1');
    settings.onConfirm('Str0ng@1');
    const changed = settings.savePassword();
    http().expectOne(`${API}/users/me/password`).flush('');
    await changed;
    expect(settings.currentPassword).toBe('');
    settings.onCurrent('OldPass@1');
    settings.onNewPassword('Str0ng@2');
    settings.onConfirm('Str0ng@2');
    const rejected = settings.savePassword();
    http().expectOne(`${API}/users/me/password`).flush('{"message":"Current password is incorrect"}', { status: 400, statusText: 'Bad' });
    await rejected;
    expect(settings.passErrors()['current']).toContain('incorrect');
    settings.onCurrent('OldPass@1');
    settings.onNewPassword('Str0ng@3');
    settings.onConfirm('Str0ng@3');
    const fielded = settings.savePassword();
    http().expectOne(`${API}/users/me/password`).flush('{"message":"Current password is incorrect","errors":{"current":"Wrong current"}}', { status: 400, statusText: 'Bad' });
    await fielded;
    expect(settings.passErrors()['current']).toBe('Current password is incorrect');
    settings.onConfirm('other');
    settings.confirmPassword = '';
    settings.onNewPassword('');
    settings.onNewPassword('Str0ng@9');
    settings.onCurrent('OldPass@1');
    settings.onConfirm('Str0ng@9');
    const plain = settings.savePassword();
    http().expectOne(`${API}/users/me/password`).flush('{"message":"Try again later"}', { status: 400, statusText: 'Bad' });
    await plain;
    expect(settings.passErrors()['current']).toBe('Try again later');
  });

  it('skips setup or saves income and an optional goal', async () => {
    resign(member('admin'));
    const adminFixture = TestBed.createComponent(SetupComponent);
    await adminFixture.componentInstance.ngOnInit();
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalled();

    resign(member());
    const fixture = TestBed.createComponent(SetupComponent);
    const setup = fixture.componentInstance;
    const loading = setup.ngOnInit();
    http().expectOne(`${API}/dashboard`).flush(JSON.stringify({ data: { finance: { monthlyIncome: 1000, investmentTypes: 'Gold, Stocks', incomeSource: 'Salary' } } }));
    await loading;
    expect(setup.investTypes.has('Gold')).toBeTrue();
    setup.toggle(setup.investTypes, 'Gold');
    setup.toggle(setup.investTypes, 'Crypto');
    expect(setup.totalIncome()).toBe(1000);
    expect(setup.incomeOk(0)).toContain('greater than zero');
    expect(setup.incomeOk('')).toContain('required');
    expect(setup.incomeOk(10)).toBe('');
    setup.touch('monthlyIncome', 'Monthly income is required');
    expect(setup.goalDateOk('2000-01-01')).toContain('past');
    expect(setup.goalDateOk('2999-01-01')).toBe('');
    expect(setup.optionalAmountOk('', 'Other income')).toBe('');
    expect(setup.optionalAmountOk(-1, 'Other income')).toContain('non-negative');
    setup.currentSavings = 10;
    setup.emergencyReserve = 50;
    expect(setup.reserveOk()).toContain('cannot be larger');
    expect(setup.choiceOk('')).toContain('required');
    expect(setup.goalTitleOk('')).toContain('required');
    expect(setup.goalTargetOk(0)).toContain('greater');
    await setup.submit();
    expect(setup.errors()['confirm']).toContain('confirm');

    setup.monthlyIncome = 1000;
    setup.confirmAccurate = true;
    setup.emergencyReserve = 5;
    setup.hasGoal = 'yes';
    setup.primaryGoal = 'Home';
    setup.goalCategory = 'Home';
    setup.goalTarget = 5000;
    setup.goalDate = '2999-01-01';
    const saved = setup.submit();
    http().expectOne(`${API}/finance`).flush(JSON.stringify({ data: { monthlyIncome: 1000 } }));
    await flushDash();
    (await whenRequested(`${API}/goals`)).flush('{"message":"Goal was not saved"}', { status: 400, statusText: 'Bad' });
    await closeNotice();
    await saved;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/budget');

    setup.hasGoal = 'no';
    const again = setup.submit();
    http().expectOne(`${API}/finance`).flush(JSON.stringify({ data: { monthlyIncome: 1000 } }));
    (await whenRequested(`${API}/dashboard`)).flush(JSON.stringify({ data: { finance: { monthlyIncome: 1000 }, budgets: [{ id: 'b', category: 'Food', limit: 10 }] } }));
    await again;
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/dashboard');
    const rejected = setup.submit();
    http().expectOne(`${API}/finance`).flush('{"message":"Check the form","errors":{"monthlyIncome":"Monthly income is required"}}', { status: 400, statusText: 'Bad' });
    await rejected;
    expect(setup.formError()).toContain('Check the form');
    const skipped = setup.skip();
    http().expectOne(`${API}/finance/skip`).flush('');
    await flushDash();
    await skipped;
  });

  it('builds every shell for the three roles', fakeAsync(() => {
    try {
    const router = TestBed.inject(Router);
    const events = new Subject<NavigationEnd>();
    Object.defineProperty(router, 'events', { configurable: true, get: () => events });
    const visibility = spyOnProperty(document, 'visibilityState', 'get').and.returnValue('hidden');
    const nested = { snapshot: { data: { title: 'Goals' } }, firstChild: null };
    Object.defineProperty(TestBed.inject(ActivatedRoute), 'firstChild', { configurable: true, value: nested });
    const memberShell = TestBed.createComponent(ShellComponent);
    memberShell.detectChanges();
    expect(memberShell.componentInstance.roleLabel()).toBe('Member');
    expect(memberShell.componentInstance.initial()).toBe('U');
    document.dispatchEvent(new Event('visibilitychange'));
    flushMicrotasks();
    flushDashNow();
    tick();
    visibility.and.returnValue('visible');
    document.dispatchEvent(new Event('visibilitychange'));
    window.dispatchEvent(new Event('focus'));
    http().match(`${API}/notifications`).forEach((req) => req.flush(JSON.stringify({ data: [] })));
    tick();
    memberShell.componentInstance.openMenu();
    expect(document.body.classList.contains('sidebar-open')).toBeTrue();
    events.next(new NavigationEnd(1, '/goals', '/goals'));
    expect(document.body.classList.contains('sidebar-open')).toBeFalse();
    memberShell.componentInstance.retryRefresh();
    flushMicrotasks();
    flushDashNow();
    tick();
    memberShell.componentInstance.retryRefresh();
    http().expectOne(`${API}/dashboard`).flush('no', { status: 500, statusText: 'Error' });
    tick();
    visibility.and.returnValue('visible');
    tick(10000);
    http().match(`${API}/notifications`).forEach((req) => req.flush('no', { status: 500, statusText: 'Error' }));
    tick();
    visibility.and.returnValue('hidden');
    tick(10000);
    memberShell.componentInstance.ngOnDestroy();
    memberShell.destroy();

    resign(member('admin'));
    TestBed.inject(WorkspaceService).loaded.set(true);
    const adminShell = TestBed.createComponent(ShellComponent);
    adminShell.detectChanges();
    expect(adminShell.componentInstance.roleLabel()).toBe('Admin');
    expect(adminShell.componentInstance.nav().some((item) => item.kind === 'link' && item.path === '/admin/users')).toBeTrue();
    adminShell.destroy();

    resign(member('superadmin'));
    const superRouter = TestBed.inject(Router);
    const superShell = TestBed.createComponent(ShellComponent);
    expect(superShell.componentInstance.roleLabel()).toBe('Super Admin');
    expect(superShell.componentInstance.initial()).toBe('A');
    expect(superShell.componentInstance.nav().some((item) => item.kind === 'link' && item.path === '/admin/admins')).toBeTrue();
    superShell.detectChanges();
    http().expectOne(`${API}/dashboard`).flush('{"message":"Login is required"}', { status: 401, statusText: 'Unauthorized' });
    flushMicrotasks();
    tick();
    expect(superRouter.navigateByUrl).toHaveBeenCalledWith('/login');
    superShell.destroy();
    } finally {
      discardPeriodicTasks();
    }
  }));

  it('saves, filters, and requests deletion of a transaction', async () => {
    const fixture = TestBed.createComponent(TransactionsComponent);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    expect(page.overrunPreview()).toBe(0);
    page.setSearch('rent');
    page.setFilter('type', 'expense');
    page.setFilter('category', 'Rent');
    page.open(null);
    await page.save();
    expect(page.errors()['name']).toContain('required');
    page.form = { ...page.form, name: 'Rent', category: 'Rent', amount: 10, date: '2026-01-02', type: 'income' };
    const created = page.save();
    http().expectOne(`${API}/transactions`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await created;
    page.open({ id: '1', name: 'Rent', category: 'Rent', type: 'expense', amount: 9000, date: page.today });
    TestBed.inject(WorkspaceService).budgets.set([{ id: 'b', category: 'Rent', limit: 100 }]);
    TestBed.inject(WorkspaceService).transactions.set([{ id: '1', name: 'Rent', category: 'Rent', type: 'expense', amount: 10, date: page.today }]);
    expect(page.overrunPreview()).toBeGreaterThan(0);
    expect(page.dateOk('')).toContain('required');
    expect(page.nameOk('')).toContain('required');
    expect(page.categoryOk('')).toContain('required');
    expect(page.amountOk(10)).toBe('');
    expect(page.amountOk(0)).toContain('greater');
    const updated = page.save();
    http().expectOne(`${API}/transactions/1`).flush('{"message":"Amount is required","errors":{"amount":"Amount is required"}}', { status: 400, statusText: 'Bad' });
    await updated;
    const cancelled = page.remove(page.form);
    TestBed.inject(DialogService).close(false);
    await cancelled;
    page.touch('name', 'Name is required');
    const removed = page.remove(page.form);
    await confirm(removed);
    http().expectOne(`${API}/admin/approvals`).flush(JSON.stringify({ data: { id: 'a', status: 'pending', requestType: 'transaction-delete', resourceId: '1', requesterId: '1' } }));
    await closeNotice();
    await removed;
    const failed = page.remove(page.form);
    await confirm(failed);
    http().expectOne(`${API}/admin/approvals`).flush('{"message":"Not allowed"}', { status: 403, statusText: 'Forbidden' });
    await closeNotice();
    await failed;
  });

  it('opens a draft goal from the affordability link', () => {
    const providers = [
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor, sessionInterceptor, successInterceptor])),
      provideHttpClientTesting(),
    ];
    const routeFor = (amount: string) => ({ snapshot: { queryParamMap: { get: (key: string) => (key === 'title' ? 'Bike' : key === 'amount' ? amount : null) } } });
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers });
    TestBed.overrideProvider(ActivatedRoute, { useValue: routeFor('0') });
    const blankAmount = TestBed.createComponent(GoalsComponent).componentInstance;
    expect(blankAmount.form.title).toBe('Bike');
    expect(blankAmount.form.target).toBe(0);
    expect(blankAmount.amountOk('')).toContain('required');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers });
    TestBed.overrideProvider(ActivatedRoute, { useValue: routeFor('12') });
    const priced = TestBed.createComponent(GoalsComponent).componentInstance;
    expect(priced.form.target).toBe(12);
    expect(priced.editing()).toBeTrue();
  });

  it('saves goals, budgets, and savings and draws their graphs', async () => {
    const goals = TestBed.createComponent(GoalsComponent);
    goals.detectChanges();
    const goalPage = goals.componentInstance;
    expect(goalPage.pct(goal())).toBe(40);
    expect(goalPage.remaining(goal())).toBe(60);
    goalPage.setSearch('home');
    goalPage.setFilter('status', 'active');
    goalPage.setFilter('category', 'Home');
    goalPage.showGraph(5);
    goalPage.open(null);
    await goalPage.save();
    expect(goalPage.errors()['title']).toContain('required');
    goalPage.form = { ...goal(), id: '', deadline: '2999-01-01', status: 'active' };
    const added = goalPage.save();
    http().expectOne(`${API}/goals`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await added;
    goalPage.open(goal());
    goalPage.form.deadline = '';
    goalPage.form.status = 'draft';
    const edited = goalPage.save();
    http().expectOne(`${API}/goals/g1`).flush('{"message":"Title is required"}', { status: 400, statusText: 'Bad' });
    await edited;
    const removedGoal = goalPage.remove(goal());
    await confirm(removedGoal);
    http().expectOne(`${API}/goals/g1`).flush('');
    await flushDash();
    await removedGoal;
    await confirm(goalPage.clearAll(), false);
    const clearedGoals = goalPage.clearAll();
    await confirm(clearedGoals);
    http().expectOne(`${API}/goals`).flush('');
    await flushDash();
    await clearedGoals;
    TestBed.inject(WorkspaceService).goals.set([goal(), { ...goal(), id: 'g2', status: '' }]);
    goals.detectChanges();
    goalPage.showGraph(0);
    goals.detectChanges();
    goalPage.showGraph(1);
    goals.detectChanges();
    goalPage.touch('title', 'Title is required');
    expect(goalPage.requiredTitle('')).toContain('required');
    expect(goalPage.targetOk(0)).toContain('greater');
    expect(goalPage.deadlineOk('2000-01-01')).toContain('past');

    const budgets = TestBed.createComponent(BudgetComponent);
    budgets.detectChanges();
    const budgetPage = budgets.componentInstance;
    expect(budgetPage.label('over')).toBe('Over budget');
    expect(budgetPage.label('warn')).toBe('Near limit');
    expect(budgetPage.label('ok')).toBe('Within limit');
    expect(budgetPage.limitOk(0)).toContain('greater');
    budgetPage.open(null);
    await budgetPage.save();
    budgetPage.form = { id: '', category: 'Food', limit: 100, note: 'weekly' };
    const budgetAdded = budgetPage.save();
    http().expectOne(`${API}/budgets`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await budgetAdded;
    budgetPage.open({ id: 'b1', category: 'Food', limit: 100, spent: 10 });
    const budgetFailed = budgetPage.save();
    http().expectOne(`${API}/budgets/b1`).flush('{"message":"Category is required"}', { status: 400, statusText: 'Bad' });
    await budgetFailed;
    const removedBudget = budgetPage.remove(budgetPage.form);
    await confirm(removedBudget);
    http().expectOne(`${API}/budgets/b1`).flush('');
    await flushDash();
    await removedBudget;
    const clearedBudgets = budgetPage.clearAll();
    await confirm(clearedBudgets);
    http().expectOne(`${API}/budgets`).flush('');
    await flushDash();
    await clearedBudgets;
    TestBed.inject(WorkspaceService).budgets.set([{ id: 'b1', category: 'Food', limit: 100, spent: 120 }]);
    budgets.detectChanges();
    expect(budgetPage.totals().overrun).toBeGreaterThan(0);
    budgetPage.search.set('food');
    budgetPage.status.set('over');
    budgetPage.touch('limit', 'Limit must be greater than zero');
    budgetPage.showGraph(0);
    budgets.detectChanges();
    budgetPage.showGraph(1);
    budgets.detectChanges();

    const savings = TestBed.createComponent(SavingsComponent);
    savings.detectChanges();
    const savingPage = savings.componentInstance;
    expect(savingPage.titleOk('')).toContain('required');
    expect(savingPage.amountOk(0)).toContain('greater');
    expect(savingPage.dateOk('')).toContain('required');
    savingPage.setSearch('deposit');
    savingPage.open(null);
    await savingPage.save();
    savingPage.form = { ...saving(), id: '' };
    const savingAdded = savingPage.save();
    http().expectOne(`${API}/savings`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await savingAdded;
    savingPage.open(saving());
    const savingFailed = savingPage.save();
    http().expectOne(`${API}/savings/${saving().id}`).flush('{"message":"Title is required"}', { status: 400, statusText: 'Bad' });
    await savingFailed;
    const removedSaving = savingPage.remove(saving());
    await confirm(removedSaving);
    http().expectOne(`${API}/savings/${saving().id}`).flush('');
    await flushDash();
    await removedSaving;
    const clearedSavings = savingPage.clearAll();
    await confirm(clearedSavings);
    http().expectOne(`${API}/savings`).flush('');
    await flushDash();
    await clearedSavings;
    TestBed.inject(WorkspaceService).savings.set([saving(), { ...saving(), id: 's2', category: '' }]);
    savings.detectChanges();
    savingPage.category.set('Deposit');
    savingPage.showGraph(0);
    savings.detectChanges();
    savingPage.showGraph(1);
    savings.detectChanges();
    savingPage.touch('title', 'Title is required');
  });

  it('checks affordability and opens a goal for a shortfall', async () => {
    const fixture = TestBed.createComponent(AffordabilityComponent);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    page.touch('itemName', 'Item name is required');
    expect(page.itemOk('')).toContain('required');
    expect(page.amountOk(10)).toBe('');
    expect(page.amountOk(0)).toContain('greater');
    expect(page.dateOk('')).toContain('required');
    await page.check();
    page.itemName = 'Bike';
    page.amount = 5000;
    page.paymentMethod = 'emi';
    page.emiMonths = 6;
    page.annualInterestPercent = 8;
    const checked = page.check();
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c', itemName: 'Bike', amount: 5000, verdict: 'Stretch', level: 'warning', priority: 'Want' } }));
    await flushDash();
    await checked;
    expect(page.result()?.itemName).toBe('Bike');
    await page.createGoal();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalled();
    page.result.set({ id: 'c', itemName: 'Bike', amount: 5000, verdict: 'Yes', level: 'success' });
    await page.createGoal();
    page.paymentMethod = 'cash';
    const failed = page.check();
    http().expectOne(`${API}/affordability/check`).flush('{"message":"Amount is required"}', { status: 400, statusText: 'Bad' });
    await failed;
    page.search.set('bike');
    page.level.set('success');
    page.result.set(null);
    TestBed.inject(WorkspaceService).affordChecks.set([]);
    await page.recheck();
    expect(page.recheckHint()).toContain('not available');
    TestBed.inject(WorkspaceService).affordChecks.set([{ id: 'c', itemName: 'Bike', amount: 5000, verdict: 'Yes', level: 'success' }]);
    const again = page.recheck();
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c2', itemName: 'Bike', amount: 5000, verdict: 'Yes', level: 'success' } }));
    await flushDash();
    await again;
    const removedCheck = page.remove({ id: 'c', itemName: 'Bike', amount: 1, verdict: 'Yes', level: 'success' });
    await confirm(removedCheck);
    http().expectOne(`${API}/affordability/c`).flush('');
    await flushDash();
    await removedCheck;
    const clearedChecks = page.clearAll();
    await confirm(clearedChecks);
    http().expectOne(`${API}/affordability`).flush('');
    await flushDash();
    await clearedChecks;
  });

  it('builds a report, downloads it, and restores a saved period', async () => {
    const fixture = TestBed.createComponent(ReportsComponent);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    page.choosePreset('custom');
    page.setFrom('2026-02-10');
    page.setTo('2026-01-01');
    await page.save();
    expect(page.valid()).toBeFalse();
    fixture.detectChanges();
    expect(page.periodTitle()).toContain('valid');
    expect(page.rows()).toEqual([]);
    page.downloadCurrent('txt');
    page.preset.set('nope' as never);
    expect(page.presetLabel()).toBe('Custom dates');
    page.choosePreset('thisMonth');
    page.choosePreset('lastMonth');
    page.choosePreset('last3Months');
    page.choosePreset('thisYear');
    page.setType('expense');
    page.setCategory('Food');
    page.setSearch('rent');
    page.reportName.set('October');
    const saved = page.save();
    http().expectOne(`${API}/reports/generate`).flush(JSON.stringify({ data: { id: 'r', type: 'October', fromDate: page.from(), toDate: page.to(), income: 0, expense: 0, net: 0, txCount: 0 } }));
    await flushDash();
    await saved;
    const failed = page.save();
    http().expectOne(`${API}/reports/generate`).flush('{"message":"Start date is required"}', { status: 400, statusText: 'Bad' });
    await failed;
    TestBed.inject(WorkspaceService).transactions.set([
      { id: '1', name: 'Salary, bonus', category: 'Salary', type: 'income', amount: 100, date: page.from() },
      { id: '2', name: 'Groceries "weekly"', category: 'Food', type: 'expense', amount: 40, date: page.from() },
    ]);
    TestBed.inject(WorkspaceService).reports.set([{ id: 'r', type: 'October', fromDate: page.from(), toDate: page.to(), income: 100, expense: 40, net: 60, txCount: 2 }]);
    fixture.detectChanges();
    page.historySearch.set('oct');
    expect(page.history().total).toBe(1);
    page.showGraph(0);
    fixture.detectChanges();
    page.showGraph(1);
    fixture.detectChanges();
    spyOn(window, 'print');
    page.downloadCurrent('csv');
    page.downloadCurrent('txt');
    const row = { id: 'r', type: 'October', fromDate: page.from(), toDate: page.to(), income: 100, expense: 40, net: 60, txCount: 2 };
    page.downloadSaved(row, 'csv');
    page.open(row);
    page.print();
    expect(page.preset()).toBe('custom');
    TestBed.inject(WorkspaceService).transactions.set([
      { id: '1', name: 'Salary', category: 'Salary', type: 'income', amount: 100, date: page.from() },
    ]);
    page.showGraph(1);
    fixture.detectChanges();
    page.downloadCurrent('txt');
    page.showGraph(0);
    fixture.detectChanges();
  });

  it('loads the dashboard plan and every graph', async () => {
    resign(member());
    TestBed.inject(WorkspaceService).finance.set({ monthlyIncome: 1000 });
    TestBed.inject(WorkspaceService).loaded.set(true);
    const fixture = TestBed.createComponent(DashboardComponent);
    const page = fixture.componentInstance;
    http().expectOne(`${API}/finance/profile/1/summary`).flush(JSON.stringify({ data: { insights: ['Keep the surplus'] } }));
    await fixture.whenStable();
    expect(page.insights()).toEqual(['Keep the surplus']);
    TestBed.inject(WorkspaceService).planStale.set(true);
    fixture.detectChanges();
    http().match(`${API}/finance/profile/1/summary`).forEach((req) => req.flush('{"message":"Summary failed"}', { status: 500, statusText: 'Error' }));
    await fixture.whenStable();
    expect(page.planError()).toContain('Summary failed');
    page.setSearch('rent');
    page.setType('expense');
    page.showGraph(0);
    TestBed.inject(WorkspaceService).stats.set({ health: 140, totalSavings: 5000, monthBudget: 100, spent: 80, activeGoals: 2, income: 1000, expenses: 80, fullName: 'From stats' });
    TestBed.inject(WorkspaceService).transactions.set([{ id: '1', name: 'Groceries', category: 'Food', type: 'expense', amount: 80, date: new Date().toISOString().slice(0, 10) }]);
    TestBed.inject(WorkspaceService).savings.set([saving()]);
    TestBed.inject(WorkspaceService).budgets.set([{ id: 'b', category: 'Food', limit: 100, spent: 80 }]);
    TestBed.inject(WorkspaceService).goals.set([goal(), { ...goal(), id: 'paused', status: 'paused' }]);
    TestBed.inject(WorkspaceService).savingsOverview.set({ allocatedSavings: 1, unallocatedLiquid: 1, emergencyReserve: 1, totalSavings: 80, investments: 1, protectedForNeeds: null, availableForWants: null });
    fixture.detectChanges();
    expect(page.name()).toBe('Ada Lovelace');
    expect(page.totalSavings()).toBe(80);
    expect(page.budgetPct()).toBeGreaterThan(0);
    expect(page.goalPreview().length).toBe(1);
    expect(page.pct(goal())).toBe(40);
    for (let index = 0; index < page.graphs().length; index++) {
      page.showGraph(index);
      fixture.detectChanges();
    }
    TestBed.inject(WorkspaceService).savingsOverview.set(null);
    TestBed.inject(WorkspaceService).budgets.set([]);
    fixture.detectChanges();
    expect(page.budgetPct()).toBe(0);
    expect(page.totalSavings()).toBe(page.position().totalSavings);
    page.prompt.set(false);
    TestBed.inject(WorkspaceService).finance.set(null);
    await page.loadPlan();
    expect(page.plan()).toBeNull();
  });

  it('manages users, admins, news, feedback, and approvals', async () => {
    const users = TestBed.createComponent(AdminUsersComponent).componentInstance;
    users.open(null);
    await users.save();
    expect(users.errors()['fullName']).toContain('3 characters');
    users.form = { ...member(), id: '' };
    users.password = 'Str0ng@1';
    const created = users.save();
    http().expectOne(`${API}/auth/register`).flush(JSON.stringify({ data: member() }));
    (await whenRequested(`${API}/admin/users/1`)).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await created;
    TestBed.inject(WorkspaceService).members.set([member(), { ...member(), id: '2', email: 'other@b.co' }]);
    users.open(member());
    users.form.email = 'other@b.co';
    await users.save();
    expect(users.errors()['email']).toBe('Email already in use');
    users.form.email = 'ada@b.co';
    users.password = '';
    expect(users.passwordOk('')).toBe('');
    expect(users.nameOk('A')).toContain('3');
    expect(users.emailOk('nope')).toContain('valid');
    expect(users.phoneOk('123')).toContain('valid');
    users.touch('fullName', 'Name is required');
    const failed = users.save();
    http().expectOne(`${API}/admin/users/1`).flush('{"message":"Email already registered"}', { status: 409, statusText: 'Conflict' });
    await failed;
    await confirm(users.remove(member()), false);
    const removed = users.remove(member());
    await confirm(removed);
    http().expectOne(`${API}/admin/users/1`).flush('no', { status: 500, statusText: 'Error' });
    await closeNotice();
    await removed;

    resign(member('superadmin'));
    const admins = TestBed.createComponent(AdminAdminsComponent).componentInstance;
    admins.open(null);
    admins.password = 'short';
    await admins.save();
    admins.form = { ...member('superadmin'), id: '', role: 'admin' };
    admins.password = 'Str0ng@1';
    const added = admins.save();
    http().expectOne(`${API}/admin/admins`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await added;
    admins.open({ ...member('admin'), id: '2' });
    admins.password = '';
    const edited = admins.save();
    http().expectOne(`${API}/admin/admins/2`).flush('{"message":"Phone is required"}', { status: 400, statusText: 'Bad' });
    await edited;
    expect(admins.nameOk('A')).toContain('3');
    expect(admins.emailOk('nope')).toContain('valid');
    expect(admins.phoneOk('')).toContain('required');
    expect(admins.passwordOk('')).toBe('');
    admins.touch('fullName', 'Name is required');
    await admins.remove(member('superadmin'));
    const removedAdmin = admins.remove({ ...member('admin'), id: '2' });
    await confirm(removedAdmin);
    spyOn(TestBed.inject(AdminService), 'deleteAdmin').and.rejectWith('nope');
    const strangeAdmin = admins.remove({ ...member('admin'), id: '9' });
    await confirm(strangeAdmin);
    await closeNotice();
    await strangeAdmin;
    http().expectOne(`${API}/admin/admins/2`).flush('');
    await flushDash();
    await removedAdmin;

    const news = TestBed.createComponent(AdminNewsComponent).componentInstance;
    news.open(null);
    await news.save();
    expect(news.titleOk('')).toContain('required');
    expect(news.bodyOk('')).toContain('required');
    news.touch('title', 'Title is required');
    news.form = { id: '', title: 'Hello', body: 'World', priority: '', status: '' };
    const posted = news.save();
    http().expectOne(`${API}/admin/news`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await posted;
    news.open({ id: 'n', title: 'Hello', body: 'World' });
    const newsFailed = news.save();
    http().expectOne(`${API}/admin/news/n`).flush('{"message":"Title is required"}', { status: 400, statusText: 'Bad' });
    await newsFailed;
    const removedNews = news.remove({ id: 'n', title: 'Hello', body: 'World' });
    await confirm(removedNews);
    http().expectOne(`${API}/admin/news/n`).flush('');
    await flushDash();
    await removedNews;

    const feedback = TestBed.createComponent(AdminFeedbackComponent).componentInstance;
    feedback.beginStatus(feedbackRow());
    const status = feedback.saveStatus();
    http().expectOne(`${API}/feedback/f`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await status;
    feedback.beginStatus(feedbackRow());
    const statusFailed = feedback.saveStatus();
    http().expectOne(`${API}/feedback/f`).flush('{"message":"Status is not supported"}', { status: 400, statusText: 'Bad' });
    await statusFailed;
    feedback.statusRow.set(null);
    await feedback.saveStatus();
    const history = feedback.showHistory(feedbackRow());
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: [] }));
    await history;
    const historyFailed = feedback.showHistory(feedbackRow());
    http().expectOne(`${API}/feedback/f/history`).flush('{"message":"Missing"}', { status: 404, statusText: 'Missing' });
    await closeNotice();
    await historyFailed;
    TestBed.inject(WorkspaceService).allFeedback.set([feedbackRow(), { ...feedbackRow(), id: '2', status: 'resolved' }]);
    expect(feedback.openCount()).toBe(1);
    expect(feedback.resolvedCount()).toBe(1);

    const approvals = TestBed.createComponent(AdminApprovalsComponent);
    flushData(`${API}/admin/approvals`, [{ id: 'a', requesterId: '1', requestType: 'transaction-delete', resourceId: 'tx', reason: 'duplicate', status: 'pending', createdAt: '2026-01-02T10:00:00' }]);
    await approvals.whenStable();
    const queue = approvals.componentInstance;
    expect(queue.typeLabel('transaction-delete')).toBe('Transaction deletion');
    expect(queue.typeLabel('other')).toBe('other');
    expect(queue.statusLabel('')).toBe('');
    expect(queue.statusLabel('pending')).toBe('Pending');
    expect(queue.date()).toBe('—');
    expect(queue.date('2026-01-02T10:00:00')).toContain('2026');
    queue.search.set('dup');
    queue.status.set('all');
    queue.type.set('');
    expect(queue.page().total).toBe(1);
    queue.type.set('transaction-delete');
    expect(queue.page().total).toBe(1);
    queue.type.set('other');
    expect(queue.page().total).toBe(0);
    queue.type.set('');
    queue.status.set('pending');
    const row = queue.rows()[0];
    expect(row.details).toBe('duplicate');
    const approved = queue.approve(row);
    http().expectOne(`${API}/admin/approvals/a/approve`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/admin/approvals`)).flush(JSON.stringify({ data: [] }));
    await approved;
    const rejected = queue.reject({ ...row, id: 'b' });
    http().expectOne(`${API}/admin/approvals/b/reject`).flush('{"message":"Already reviewed"}', { status: 409, statusText: 'Conflict' });
    await rejected;
    const reload = queue.load();
    http().expectOne(`${API}/admin/approvals`).flush('{"message":"Down"}', { status: 503, statusText: 'Down' });
    await reload;
    expect(queue.error()).toContain('Down');
  });

  it('draws the admin overview and filters members', () => {
    TestBed.inject(WorkspaceService).insights.set({
      totalMembers: 2, activeMembers: 1, inactiveMembers: 1, adminCount: 1,
      setupDone: 1, setupPending: 1, feedbackTotal: 2, feedbackOpen: 1,
      newsTotal: 1, publishedNews: 1, joinLabels: ['Jan'], joinValues: [1],
      statusLabels: ['Active'], statusValues: [1], setupLabels: ['Done'], setupValues: [1],
      recentMembers: [member()],
    });
    TestBed.inject(WorkspaceService).members.set([member()]);
    const fixture = TestBed.createComponent(AdminOverviewComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    for (let index = 0; index < page.pages.length; index++) {
      page.showGraph(index);
      fixture.detectChanges();
    }
    page.search.set('Ada');
    page.status.set('active');
    expect(page.page().total).toBe(1);
    page.status.set('missing');
    expect(page.page().total).toBe(0);
    page.search.set('nobody');
    expect(page.page().rows).toEqual([]);
    TestBed.inject(WorkspaceService).insights.set({
      totalMembers: 0, activeMembers: 0, inactiveMembers: 0, adminCount: 0,
      setupDone: 0, setupPending: 0, feedbackTotal: 0, feedbackOpen: 0,
      newsTotal: 0, publishedNews: 0, joinLabels: [], joinValues: [],
      statusLabels: [], statusValues: [], setupLabels: [], setupValues: [], recentMembers: [],
    });
    fixture.detectChanges();
    page.showGraph(0);
    fixture.detectChanges();
    expect(page.graphEmpty()).toBe('Member status counts are not available.');
    page.showGraph(1);
    fixture.detectChanges();
    expect(page.graphEmpty()).toBe('Setup counts are not available.');
    page.showGraph(2);
    fixture.detectChanges();
    expect(page.graphEmpty()).toBe('No join history is available for this graph.');
    page.showGraph(3);
    fixture.detectChanges();
    expect(page.graphEmpty()).toBe('');
  });

  it('saves feedback, reads news, and clears notifications', async () => {
    const feedback = TestBed.createComponent(FeedbackComponent).componentInstance;
    feedback.touch('subject', 'Subject is required');
    feedback.search.set('idea');
    feedback.status.set('open');
    feedback.category.set('Feature');
    expect(feedback.badge('resolved')).toBe('badge-success');
    expect(feedback.badge('in-review')).toBe('badge-warning');
    expect(feedback.badge('closed')).toBe('badge-neutral');
    expect(feedback.badge('open')).toBe('badge-info');
    feedback.open(null);
    await feedback.save();
    expect(feedback.subjectOk('')).toContain('required');
    expect(feedback.messageOk('')).toContain('required');
    feedback.form = { ...feedbackRow(), id: '' };
    const added = feedback.save();
    http().expectOne(`${API}/feedback`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await added;
    feedback.open(feedbackRow());
    const failed = feedback.save();
    http().expectOne(`${API}/feedback/f`).flush('{"message":"Subject is required"}', { status: 400, statusText: 'Bad' });
    await failed;
    const history = feedback.showHistory(feedbackRow());
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: [{ action: 'opened' }] }));
    await history;
    const missing = feedback.showHistory(feedbackRow());
    http().expectOne(`${API}/feedback/f/history`).flush('{"message":"Missing"}', { status: 404, statusText: 'Missing' });
    await closeNotice();
    await missing;

    TestBed.inject(WorkspaceService).news.set([{ id: 'n', title: 'Hello', body: 'World', priority: 'high', authorName: 'Ada' }]);
    const news = TestBed.createComponent(NewsComponent).componentInstance;
    news.search.set('hello');
    news.priority.set('high');
    news.selected.set({ id: 'n', title: 'Hello', body: 'World' });
    expect(news.page().total).toBe(1);
    news.priority.set('low');
    expect(news.page().total).toBe(0);

    TestBed.inject(WorkspaceService).notifications.set([{ id: '1', title: 'Hi', message: 'There', type: 'info', read: false }]);
    const notices = TestBed.createComponent(NotificationsComponent).componentInstance;
    notices.search.set('hi');
    notices.type.set('info');
    notices.read.set('false');
    expect(notices.page().total).toBe(1);
    const notice = notices.rows()[0];
    const opened = notices.open(notice);
    http().expectOne(`${API}/notifications/1/read`).flush('');
    (await whenRequested(`${API}/notifications`)).flush(JSON.stringify({ data: [] }));
    await opened;
    await notices.open({ ...notice, read: 'true' });
    const event = { stopPropagation: jasmine.createSpy('stop') } as unknown as Event;
    await confirm(notices.remove(notice, event), false);
    expect(event.stopPropagation).toHaveBeenCalled();
    const removed = notices.remove(notice, event);
    await confirm(removed);
    http().expectOne(`${API}/notifications/1`).flush('');
    (await whenRequested(`${API}/notifications`)).flush(JSON.stringify({ data: [] }));
    await removed;
    const cleared = notices.clearAll();
    await confirm(cleared);
    http().expectOne(`${API}/notifications`).flush('');
    (await whenRequested(`${API}/notifications`)).flush(JSON.stringify({ data: [] }));
    await cleared;
  });

  it('prompts a new member to finish setup', fakeAsync(() => {
    const dash = TestBed.createComponent(DashboardComponent);
    dash.detectChanges();
    expect(dash.componentInstance.name()).toBe('Member');
    TestBed.inject(WorkspaceService).stats.set({ fullName: 'From stats', health: 0, totalSavings: 0, monthBudget: 0, spent: 0, activeGoals: 0, income: 0, expenses: 0 });
    expect(dash.componentInstance.name()).toBe('From stats');
    TestBed.inject(AuthService).user.set(member());
    expect(dash.componentInstance.name()).toBe('Ada Lovelace');
    TestBed.inject(WorkspaceService).goals.set([{ ...goal(), status: '' }]);
    expect(dash.componentInstance.goalPreview()).toEqual([]);
    dash.componentInstance.plan.set({ insights: 'note' });
    expect(dash.componentInstance.insights()).toEqual([]);
    dash.componentInstance.plan.set({ insights: ['Save more'] });
    expect(dash.componentInstance.insights()).toEqual(['Save more']);
    tick(250);
    expect(dash.componentInstance.prompt()).toBeTrue();
    dash.destroy();
    discardPeriodicTasks();
  }));

  it('covers the remaining form and filter branches', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.finance.set({ monthlyIncome: 1000 });
    workspace.goals.set([{ ...goal(), status: 'active', plannedContribution: 0 }]);
    expect(TestBed.inject(FinanceService).capacity()?.goalPlans).toBe(0);
    expect(monthExpenseTotal([{ id: '1', type: 'expense', amount: Number.NaN, date: '2026-10-02' }], '2026-10')).toBe(0);

    const login = TestBed.createComponent(LoginComponent).componentInstance;
    login.email = 'ada@b.co';
    await login.submit();
    login.password = 'short';
    await login.submit();
    expect(login.errors()['password']).toContain('8');
    login.password = 'Str0ng@1';
    const rejected = login.submit();
    http().expectOne(`${API}/auth/login`).flush(JSON.stringify({ ok: false }));
    await rejected;
    expect(login.formError()).toContain('could not sign you in');
    spyOn(TestBed.inject(AuthService), 'login').and.resolveTo({ ok: true });
    await login.submit();
    expect(login.formError()).toContain('could not sign you in');

    const forgot = TestBed.createComponent(ForgotComponent).componentInstance;
    forgot.onPassword('Str0ng@1');
    forgot.onOtp('123456');
    forgot.email = 'ada@b.co';
    const sent = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush(JSON.stringify({ data: {} }));
    await closeNotice();
    await sent;
    const sentFailed = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush('{"message":"Unknown email"}', { status: 404, statusText: 'Missing' });
    await sentFailed;
    forgot.otp = '123456';
    forgot.password = 'Str0ng@1';
    forgot.confirmPassword = 'Str0ng@1';
    const reset = forgot.submit();
    http().expectOne(`${API}/auth/forgot/reset`).flush('{"message":"Code expired"}', { status: 400, statusText: 'Bad' });
    await reset;
    expect(forgot.errors()['otp']).toContain('expired');

    TestBed.inject(AuthService).user.set(member());
    workspace.loaded.set(true);
    workspace.finance.set(null);
    const setup = TestBed.createComponent(SetupComponent).componentInstance;
    await setup.ngOnInit();
    expect(setup.monthlyIncome).toBeNull();
    workspace.finance.set({ monthlyIncome: 0, incomeSource: '', additionalIncome: 0, currentSavings: 0, emergencyReserve: 0, investments: 0, payCycle: '', savingsType: '', investmentTypes: '' });
    await setup.ngOnInit();
    expect(setup.monthlyIncome).toBeNull();
    expect(setup.choiceOk('Salary')).toBe('');
    expect(setup.goalTargetOk(20)).toBe('');
    setup.hasGoal = 'yes';
    setup.monthlyIncome = 1000;
    setup.confirmAccurate = true;
    await setup.submit();
    expect(setup.errors()['goalCategory']).toContain('Select');
    expect(setup.errors()['goalTarget']).toContain('greater');

    const users = TestBed.createComponent(AdminUsersComponent).componentInstance;
    expect(users.page().total).toBe(0);
    users.open({ ...member(), id: '9', phone: undefined, status: '' });
    users.form.phone = undefined;
    users.password = 'Str0ng@1';
    await users.save();
    users.form.phone = '9876543210';
    users.form.status = '';
    expect(users.passwordOk('Str0ng@1')).toBe('');
    users.form.id = '';
    expect(users.passwordOk('')).toContain('required');
    users.form.id = '9';
    const savedUser = users.save();
    http().expectOne(`${API}/admin/users/9`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await savedUser;
    spyOn(TestBed.inject(AdminService), 'deleteMember').and.rejectWith('nope');
    const removedUser = users.remove({ ...member(), id: '9' });
    await confirm(removedUser);
    await closeNotice();
    await removedUser;

    TestBed.inject(AuthService).user.set({ ...member('superadmin'), id: '' });
    const admins = TestBed.createComponent(AdminAdminsComponent).componentInstance;
    expect(admins.me()).toBe('');
    expect(admins.page().total).toBe(0);
    expect(admins.nameOk('Ada Lovelace')).toBe('');
    expect(admins.emailOk('ada@b.co')).toBe('');
    expect(admins.phoneOk('9876543210')).toBe('');
    admins.touch('fullName', 'Name is required');
    admins.open({ ...member('admin'), id: '2', phone: undefined, status: '' });
    admins.password = 'short';
    await admins.save();
    admins.password = 'Str0ng@1';
    admins.form.phone = '9876543210';
    admins.form.status = '';
    const savedAdmin = admins.save();
    http().expectOne(`${API}/admin/admins/2`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await savedAdmin;
    spyOn(TestBed.inject(AdminService), 'deleteAdmin').and.rejectWith('nope');
    const removedAdmin = admins.remove({ ...member('admin'), id: '3' });
    await confirm(removedAdmin);
    await closeNotice();
    await removedAdmin;
    (TestBed.inject(AdminService).deleteAdmin as jasmine.Spy).and.rejectWith(new Error('blocked'));
    const erroredAdmin = admins.remove({ ...member('admin'), id: '4' });
    await confirm(erroredAdmin);
    await closeNotice();
    await erroredAdmin;
    expect(admins.passwordOk('Str0ng@1')).toBe('');

    const news = TestBed.createComponent(AdminNewsComponent).componentInstance;
    expect(news.page().total).toBe(0);

    const adminFeedback = TestBed.createComponent(AdminFeedbackComponent).componentInstance;
    const history = adminFeedback.showHistory({ ...feedbackRow(), userName: undefined });
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: [] }));
    await history;
    expect(adminFeedback.historyMeta.endsWith('· ')).toBeTrue();

    const approvals = TestBed.createComponent(AdminApprovalsComponent).componentInstance;
    (await whenRequested(`${API}/admin/approvals`)).flush(JSON.stringify({ data: [{ id: 'a', requesterId: '1', requestType: 'transaction-delete', resourceId: 'tx', status: 'pending' }] }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(approvals.rows()[0].details).toBe('tx');
    expect(approvals.page().total).toBe(1);
    approvals.status.set('all');
    expect(approvals.page().total).toBe(1);

    const tx = TestBed.createComponent(TransactionsComponent).componentInstance;
    tx.form = { ...tx.form, type: 'income' };
    expect(tx.overrunPreview()).toBe(0);
    expect(tx.dateOk(tx.today)).toBe('');
    expect(tx.amountOk(10)).toBe('');
    tx.form = { ...tx.form, name: 'Rent', category: 'Rent', amount: 10, date: tx.today, type: 'income' };
    const created = tx.save();
    http().expectOne(`${API}/transactions`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await created;

    const budget = TestBed.createComponent(BudgetComponent).componentInstance;
    expect(budget.limitOk(10)).toBe('');
    budget.form = { id: '', category: 'Food', limit: 10, note: '' };
    const budgetSaved = budget.save();
    http().expectOne(`${API}/budgets`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await budgetSaved;

    const savingPage = TestBed.createComponent(SavingsComponent).componentInstance;
    expect(savingPage.amountOk(10)).toBe('');
    savingPage.open({ ...saving(), date: '' });
    savingPage.form.date = '2026-01-04';
    savingPage.form.note = '';
    savingPage.form.goalId = '';
    const savingSaved = savingPage.save();
    http().expectOne(`${API}/savings/${saving().id}`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await savingSaved;

    const goalPage = TestBed.createComponent(GoalsComponent).componentInstance;
    expect(goalPage.targetOk(10)).toBe('');
    expect(goalPage.deadlineOk('')).toContain('required');
    expect(goalPage.deadlineOk('2999-01-01')).toBe('');
    goalPage.open({ ...goal(), deadline: '' });
    await goalPage.save();
    goalPage.form.status = 'draft';
    goalPage.form.deadline = '2999-01-01';
    const dated = goalPage.save();
    http().expectOne(`${API}/goals/${goal().id}`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await dated;
    goalPage.form.deadline = '';
    goalPage.form.plannedContribution = 0;
    goalPage.form.description = '';
    goalPage.form.priority = '';
    const goalSaved = goalPage.save();
    http().expectOne(`${API}/goals/${goal().id}`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await goalSaved;

    const afford = TestBed.createComponent(AffordabilityComponent).componentInstance;
    expect(afford.dateOk('2999-01-01')).toContain('future');
    expect(afford.dateOk(afford.today)).toBe('');
    expect(afford.amountOk(10)).toBe('');
    expect(afford.amountOk(0)).toContain('greater');
    afford.itemName = 'Bike';
    afford.amount = 10;
    afford.paymentMethod = 'emi';
    afford.emiMonths = 3;
    afford.annualInterestPercent = 0;
    const checked = afford.check();
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c', itemName: 'Bike', amount: 10, verdict: 'Yes', level: 'success' } }));
    await flushDash();
    await checked;

    const feedback = TestBed.createComponent(FeedbackComponent).componentInstance;
    expect(feedback.page().total).toBe(0);
    feedback.form = { ...feedbackRow(), status: '' };
    const feedbackSaved = feedback.save();
    http().expectOne(`${API}/feedback/f`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await feedbackSaved;

    const settings = TestBed.createComponent(SettingsComponent).componentInstance;
    settings.onNewPassword('Str0ng@1');

    const reports = TestBed.createComponent(ReportsComponent);
    reports.detectChanges();
    const reportPage = reports.componentInstance;
    reportPage.choosePreset('thisMonth');
    workspace.transactions.set([
      { id: '1', name: 'A, "B"', category: 'Food', type: 'expense', amount: 10, date: reportPage.from() },
      { id: '2', name: null as unknown as string, category: 'Food', type: 'expense', amount: 1, date: reportPage.from() },
    ]);
    reportPage.showGraph(1);
    reports.detectChanges();
    reportPage.downloadCurrent('csv');
  });

  it('covers the remaining form, chart, and fallback branches', async () => {
    resign(member());
    const dash = TestBed.createComponent(DashboardComponent);
    const board = dash.componentInstance;
    await new Promise((resolve) => setTimeout(resolve, 300));
    expect(board.prompt()).toBeTrue();
    TestBed.inject(WorkspaceService).stats.set({ health: 1, totalSavings: 1, monthBudget: 1, spent: 1, activeGoals: 0, income: 1, expenses: 1, fullName: 'From stats' });
    TestBed.inject(AuthService).patchUser({ fullName: '' });
    expect(board.name()).toBe('From stats');
    TestBed.inject(WorkspaceService).stats.set(null);
    expect(board.name()).toBe('Member');
    board.plan.set({ insights: 'nope' });
    expect(board.insights()).toEqual([]);
    TestBed.inject(WorkspaceService).goals.set([{ ...goal(), status: '' }]);
    expect(board.goalPreview()).toEqual([]);
    expect(board.saveWidth()).toBeGreaterThanOrEqual(0);
    expect(board.budgetRows()).toEqual([]);

    const login = TestBed.createComponent(LoginComponent).componentInstance;
    login.email = 'ada@b.co';
    login.password = '';
    await login.submit();
    expect(login.errors()['password']).toContain('required');
    login.password = 'Str0ng@1';
    const denied = login.submit();
    http().expectOne(`${API}/auth/login`).flush('{"message":"Invalid email or password"}', { status: 401, statusText: 'Unauthorized' });
    await denied;
    expect(login.errors()['email']).toContain('Invalid email');
    spyOn(TestBed.inject(AuthService), 'login').and.resolveTo({ ok: false });
    login.password = 'Str0ng@1';
    await login.submit();
    expect(login.formError()).toContain('could not sign you in');

    const forgot = TestBed.createComponent(ForgotComponent).componentInstance;
    forgot.email = 'ada@b.co';
    forgot.onOtp('');
    forgot.onOtp('123456');
    forgot.onPassword('Str0ng@1');
    forgot.onConfirm('Str0ng@1');
    forgot.onPassword('Str0ng@1');
    const sent = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush(JSON.stringify({ data: {} }));
    await closeNotice();
    await sent;
    const mailbox = forgot.sendCode();
    http().expectOne(`${API}/auth/forgot/send-otp`).flush('{"message":"Mailbox missing"}', { status: 404, statusText: 'Missing' });
    await mailbox;
    expect(forgot.errors()['email']).toContain('Mailbox');
    forgot.email = 'ada@b.co';
    forgot.otp = '123456';
    forgot.password = 'Str0ng@1';
    forgot.confirmPassword = 'nope';
    await forgot.submit();
    expect(forgot.errors()['confirmPassword']).toContain('match');
    forgot.onConfirm('Str0ng@1');
    const resetFailed = forgot.submit();
    http().expectOne(`${API}/auth/forgot/reset`).flush('{"message":"Reset failed"}', { status: 400, statusText: 'Bad' });
    await resetFailed;
    expect(forgot.errors()['otp']).toContain('Reset');

    const users = TestBed.createComponent(AdminUsersComponent).componentInstance;
    users.open({ ...member(), status: undefined, phone: undefined });
    expect(users.form.status).toBe('active');
    users.password = 'short';
    await users.save();
    expect(users.phoneOk(undefined as unknown as string)).toContain('required');
    users.password = '';
    expect(users.passwordOk('Str0ng@1')).toBe('');
    users.form = { ...member(), status: '' };
    users.password = '';
    const kept = users.save();
    http().expectOne(`${API}/admin/users/1`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await kept;
    spyOn(TestBed.inject(AdminService), 'deleteMember').and.rejectWith('nope');
    const strange = users.remove(member());
    await confirm(strange);
    await closeNotice();
    await strange;

    TestBed.inject(WorkspaceService).allNews.set([{ id: 'n', title: 'Hello', body: 'World', priority: 'high', status: 'published', authorName: 'Ada' }]);
    const news = TestBed.createComponent(AdminNewsComponent).componentInstance;
    news.search.set('hello');
    news.status.set('published');
    news.priority.set('high');
    expect(news.page().total).toBe(1);
    news.open({ id: 'n', title: 'Hello', body: 'World' });
    expect(news.form.priority).toBe('normal');
    expect(news.form.status).toBe('published');

    const feedback = TestBed.createComponent(FeedbackComponent).componentInstance;
    feedback.form = { ...feedbackRow(), id: '', status: '' };
    const added = feedback.save();
    http().expectOne(`${API}/feedback`).flush(JSON.stringify({ data: {} }));
    await flushDash();
    await added;
    TestBed.inject(WorkspaceService).feedback.set([feedbackRow()]);
    feedback.search.set('idea');
    expect(feedback.page().total).toBeGreaterThanOrEqual(0);

    const adminFeedback = TestBed.createComponent(AdminFeedbackComponent).componentInstance;
    TestBed.inject(WorkspaceService).allFeedback.set([feedbackRow()]);
    adminFeedback.search.set('idea');
    adminFeedback.status.set('open');
    expect(adminFeedback.page().total).toBeGreaterThanOrEqual(0);
    const unnamed = adminFeedback.showHistory({ ...feedbackRow(), userName: '' });
    http().expectOne(`${API}/feedback/f/history`).flush(JSON.stringify({ data: [] }));
    await unnamed;
    expect(adminFeedback.historyMeta).toContain('·');

    const queue = TestBed.createComponent(AdminApprovalsComponent);
    (await whenRequested(`${API}/admin/approvals`)).flush(JSON.stringify({ data: [{ id: 'a', requesterId: '1', requestType: 'transaction-delete', resourceId: 'tx', status: 'pending' }] }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    const approval = queue.componentInstance;
    expect(approval.rows()[0].details).toBe('tx');
    expect(approval.page().total).toBe(1);
    const failedApproval = approval.approve(approval.rows()[0]);
    http().expectOne(`${API}/admin/approvals/a/approve`).flush('{"message":"no"}', { status: 500, statusText: 'Bad' });
    await failedApproval;
    const rejectedApproval = approval.reject(approval.rows()[0]);
    http().expectOne(`${API}/admin/approvals/a/reject`).flush('');
    (await whenRequested(`${API}/admin/approvals`)).flush(JSON.stringify({ data: [] }));
    await rejectedApproval;

    const afford = TestBed.createComponent(AffordabilityComponent).componentInstance;
    afford.itemName = 'Bike';
    afford.amount = 10;
    afford.paymentMethod = 'emi';
    afford.emiMonths = 3;
    afford.annualInterestPercent = 0;
    const checked = afford.check();
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c', itemName: 'Bike', amount: 10, verdict: 'Yes', level: 'success' } }));
    await flushDash();
    await checked;
    TestBed.inject(WorkspaceService).affordChecks.set([{ id: 'c', itemName: 'Bike', amount: 10, verdict: 'Yes', level: 'success' }]);
    const again = afford.recheck();
    http().expectOne(`${API}/affordability/check`).flush(JSON.stringify({ data: { id: 'c2', itemName: 'Bike', amount: 10, verdict: 'Yes', level: 'success' } }));
    await flushDash();
    await again;

    const reports = TestBed.createComponent(ReportsComponent);
    const report = reports.componentInstance;
    report.setFrom('2026-01-01');
    report.setTo('2026-06-30');
    report.setType('all');
    report.setCategory('all');
    TestBed.inject(WorkspaceService).transactions.set([
      { id: '1', name: 'Rent', category: 'Rent', type: 'expense', amount: 10, date: '2026-01-02' },
      { id: '2', name: null as unknown as string, category: 'Salary', type: 'income', amount: 20, date: '2026-03-02' },
    ]);
    reports.detectChanges();
    expect(report.analysis().trend.unit).toBe('month');
    expect(report.rows().some((row) => row.name == null)).toBeTrue();
    report.showGraph(1);
    reports.detectChanges();
    report.downloadCurrent('csv');

    const budgets = TestBed.createComponent(BudgetComponent);
    const budgetPage = budgets.componentInstance;
    budgetPage.form = { id: '', category: 'Food', limit: 10, note: '' };
    const budgetWarned = budgetPage.save();
    http().expectOne(`${API}/budgets`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await budgetWarned;

    const savings = TestBed.createComponent(SavingsComponent);
    const savingPage = savings.componentInstance;
    savingPage.open({ ...saving(), date: '', note: undefined as unknown as string, goalId: undefined as unknown as string });
    expect(savingPage.form.date).toBe('');
    savingPage.form = { ...saving(), id: '', note: '', goalId: '' };
    const savingWarned = savingPage.save();
    http().expectOne(`${API}/savings`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await savingWarned;

    const goals = TestBed.createComponent(GoalsComponent);
    const goalPage = goals.componentInstance;
    expect(goalPage.amountOk(1)).toBe('');
    expect(goalPage.deadlineOk('')).toContain('required');
    expect(goalPage.targetOk(10)).toBe('');
    goalPage.form = { ...goal(), id: '', status: 'draft', deadline: '', plannedContribution: 0, description: '', priority: '' };
    const goalAdded = goalPage.save();
    http().expectOne(`${API}/goals`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await goalAdded;

    const txs = TestBed.createComponent(TransactionsComponent).componentInstance;
    txs.form = { id: '', name: 'Pay', category: 'Salary', type: 'income', amount: 10, date: txs.today };
    expect(txs.overrunPreview()).toBe(0);
    txs.form = { ...txs.form, id: 't1', type: 'expense', category: 'Rent', name: 'Rent' };
    expect(txs.overrunPreview()).toBeGreaterThanOrEqual(0);
    txs.form.id = '';
    const txWarned = txs.save();
    http().expectOne(`${API}/transactions`).flush(JSON.stringify({ data: {} }));
    (await whenRequested(`${API}/dashboard`)).flush('down', { status: 503, statusText: 'Down' });
    await closeNotice();
    await txWarned;

    resign(member());
    const sparse = TestBed.createComponent(SetupComponent).componentInstance;
    const loading = sparse.ngOnInit();
    http().expectOne(`${API}/dashboard`).flush(JSON.stringify({ data: { finance: { monthlyIncome: 0 } } }));
    await loading;
    expect(sparse.monthlyIncome).toBeNull();
    expect(sparse.incomeSource).toBe('');
    expect(sparse.choiceOk('yes')).toBe('');
    expect(sparse.goalTargetOk(10)).toBe('');
    sparse.hasGoal = 'yes';
    sparse.monthlyIncome = 1000;
    sparse.confirmAccurate = true;
    await sparse.submit();
    expect(sparse.errors()['goalCategory']).toContain('category');
  });
});

describe('bootstrap', () => {
  it('logs a failure to start the application', async () => {
    const { startMicroVault } = await import('../../main');
    spyOn(console, 'error');
    await startMicroVault(() => Promise.reject(new Error('boot failed')));
    expect(console.error).toHaveBeenCalled();
    await startMicroVault(() => Promise.resolve(undefined as never));
  });
});

function boot(user?: VaultUser): void {
  localStorage.clear();
  sessionStorage.clear();
  if (user) {
    localStorage.setItem('mv_token', 'tok');
    localStorage.setItem('mv_currentUser', JSON.stringify(user));
    localStorage.setItem('mv_isLoggedIn', 'true');
  }
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor, sessionInterceptor, successInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const router = TestBed.inject(Router);
  spyOn(router, 'navigateByUrl').and.resolveTo(true);
  spyOn(router, 'navigate').and.resolveTo(true);
}

function resign(user?: VaultUser): void {
  TestBed.resetTestingModule();
  boot(user);
}

function http(): HttpTestingController {
  return TestBed.inject(HttpTestingController);
}

function flushDashNow(): void {
  const pending = http().match(`${API}/dashboard`);
  expect(pending.length).toBeGreaterThan(0);
  pending.forEach((request) => request.flush(JSON.stringify({ data: {} })));
}

async function whenRequested(url: string) {
  for (let attempt = 0; attempt < 30; attempt++) {
    const found = http().match(url);
    if (found.length) return found[found.length - 1];
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
  return http().expectOne(url);
}

async function flushDash(): Promise<void> {
  (await whenRequested(`${API}/dashboard`)).flush(JSON.stringify({ data: {} }));
}

function flushData(url: string, data: unknown): void {
  http().expectOne(url).flush(JSON.stringify({ data }));
}

async function expectWrite(run: () => Promise<unknown>, url: string, method: string): Promise<void> {
  const pending = run();
  const request = http().expectOne(url);
  expect(request.request.method).toBe(method);
  request.flush(JSON.stringify({ data: {} }));
  await Promise.resolve();
  await flushDash();
  await pending;
}

async function closeNotice(): Promise<void> {
  const dialog = TestBed.inject(DialogService);
  for (let attempt = 0; attempt < 8 && !dialog.state(); attempt++) {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
  dialog.close(true);
  await Promise.resolve();
}

function runGuard(guard: typeof authGuard): unknown {
  return TestBed.runInInjectionContext(() => guard({} as never, {} as never));
}

function member(role = 'user'): VaultUser {
  return { id: '1', fullName: 'Ada Lovelace', email: 'ada@b.co', phone: '9876543210', role, status: 'active' };
}

function person() {
  return { fullName: 'Ada Lovelace', email: 'ada@b.co', phone: '9876543210', password: 'Str0ng@1', confirmPassword: 'Str0ng@1' };
}

function goal(): Goal {
  return { id: 'g1', title: 'Home', category: 'Home', target: 100, saved: 40, deadline: '2999-01-01', status: 'active', plannedContribution: 10 };
}

function saving(): Saving {
  return { id: 's1', title: 'Deposit', category: 'Deposit', amount: 25, date: '2026-01-04', note: 'bonus' };
}

function feedbackRow(): Feedback {
  return { id: 'f', subject: 'Idea', category: 'Feature', message: 'Please add export', status: 'open', userName: 'Ada' };
}

function payload() {
  return {
    user: member(),
    finance: { monthlyIncome: 1000 },
    setupSkipped: false,
    stats: { health: 20, totalSavings: 10, monthBudget: 100, spent: 80, activeGoals: 1, income: 1000, expenses: 80 },
    transactions: [{ id: 't', name: 'Rent', category: 'Rent', type: 'expense', amount: 10, date: '2026-01-02' }],
    goals: [goal()],
    savings: [saving()],
    savingsOverview: { allocatedSavings: 10, unallocatedLiquid: 5, emergencyReserve: 5, totalSavings: 20, investments: 1, protectedForNeeds: 30, availableForWants: 0 },
    budgets: [
      { id: '1', category: 'Food', limit: 100, spent: 1, usage: 'over budget' },
      { id: '2', category: 'Rent', limit: 100, spent: 1, usage: 'close' },
      { id: '3', category: 'EMI', limit: 100, spent: 1, usage: 'near' },
      { id: '4', category: 'Travel', limit: 100, spent: 150 },
      { id: '5', category: 'Care', limit: 100, spent: 80 },
      { id: '6', category: 'Fun', limit: 100, spent: 10 },
      { id: '7', category: 'Other', limit: 0, spent: 10 },
    ],
    reports: [],
    affordChecks: [],
    notifications: [{ id: 'n1', title: 'Hi', message: 'There', type: 'info', read: false }],
    feedback: [{ id: 'f', subject: 'S', message: 'M', status: 'open' }],
    news: [{ id: 'n', title: 'T', body: 'B' }],
    members: [member()],
    admins: [{ ...member(), id: '2', role: 'admin' }],
    adminInsights: { totalMembers: 1, recentMembers: [member()], joinLabels: ['Jan'], joinValues: [1] },
  };
}

function key(name: string): KeyboardEvent {
  return { key: name, preventDefault() {} } as KeyboardEvent;
}

function rect(): DOMRect {
  return { left: 0, top: 0, width: 180, height: 180, right: 180, bottom: 180, x: 0, y: 0, toJSON: () => ({}) };
}

async function confirm(pending: Promise<unknown>, accept = true): Promise<void> {
  TestBed.inject(DialogService).close(accept);
  await Promise.resolve();
  if (!accept) await pending;
}
