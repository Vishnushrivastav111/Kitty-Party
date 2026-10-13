import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { describe, it } from 'node:test';
import { ApiError, parseApiError } from './api-error.ts';
import { assignError, confirmError, mergeApiFields } from './field-state.ts';
import { Validators } from './format.ts';
import {
  API_CAPABILITIES,
  applyConfirmedDeletion,
  categoryRemaining,
  dashboardGraphs,
  expenseBreakdown,
  availableSavings,
  buildFinancialPosition,
  goalProgress,
  incomeExpenseSeries,
  joinFullName,
  monthExpenseTotal,
  monthExpenseTotalWithDraft,
  monthlyBudgetPosition,
  overrunIncrease,
  personName,
  profileSurplus,
  reportDateErrors,
  reportRequestBody,
  savingsTrend,
  splitFullName,
  stepGraph,
  totalsFromTransactions,
  transactionDeletionRequest,
  transactionsMatching,
} from './finance-rules.ts';

const root = new URL('../', import.meta.url);

describe('API errors', () => {
  it('keeps the backend login message', () => {
    const error = parseApiError({ status: 401, error: JSON.stringify({ message: 'Invalid email or password', path: '/api/auth/login' }) });
    assert.equal(error.message, 'Invalid email or password');
    assert.equal(error.message.includes('/api/'), false);
    assert.equal(error.status, 401);
  });

  it('places a full-name API error beside the name fields', () => {
    const errors = mergeApiFields({}, { fullName: 'Full name is required' });
    assert.equal(errors['firstName'], 'Full name is required');
    assert.equal(errors['lastName'], 'Full name is required');
  });

  it('maps registration validation text onto fields', () => {
    const error = parseApiError({
      status: 400,
      error: { message: 'Full name is required; Enter a valid email address', path: '/api/auth/register' },
    });
    assert.equal(error.fieldErrors['fullName'], 'Full name is required');
    assert.equal(error.fieldErrors['email'], 'Enter a valid email address');
    assert.equal(error.message.includes('Full name is required'), true);
  });

  it('reads structured field errors and ignores secrets', () => {
    const error = parseApiError({
      status: 400,
      error: { message: 'Check the form', errors: { email: 'Already registered', password: 'secret-value' } },
    });
    assert.equal(error.fieldErrors['email'], 'Already registered');
    assert.equal(error.fieldErrors['password'], undefined);
  });

  it('handles an empty body and a network failure', () => {
    const empty = parseApiError({ status: 500, error: '' });
    assert.equal(empty.message, 'The server returned status 500 without a message.');
    const offline = parseApiError({ status: 0, error: null });
    assert.equal(offline.status, 0);
    assert.equal(offline.message.includes('could not be reached'), true);
  });

  it('does not surface HTML, URLs, or tokens', () => {
    const html = parseApiError({ status: 502, error: '<html><body>gateway</body></html>' });
    assert.equal(html.message.includes('<html'), false);
    assert.equal(html.message, 'The server returned status 502 without a message.');
    const leaked = parseApiError({ status: 400, error: { message: 'bad token=abc password=secret bearer eyJabc.def.ghi http://localhost:8080/api/auth/login' } });
    assert.equal(leaked.message.includes('secret'), false);
    assert.equal(leaked.message.includes('localhost'), false);
    assert.equal(leaked.message.includes('eyJ'), false);
  });

  it('does not replace a supplied message with a generic one', () => {
    const error = parseApiError({ status: 409, error: 'Email already registered' });
    assert.equal(error.message, 'Email already registered');
    assert.ok(error instanceof ApiError);
  });
});

describe('validation', () => {
  it('shows and clears a mobile number error', () => {
    assert.equal(Validators.phone('123'), 'Enter a valid mobile number');
    let errors = assignError({}, 'phone', Validators.phone('123'));
    assert.equal(errors['phone'], 'Enter a valid mobile number');
    errors = assignError(errors, 'phone', Validators.phone('9876543210'));
    assert.equal(errors['phone'], undefined);
  });

  it('clears an email error when the value becomes valid', () => {
    let errors = assignError({}, 'email', Validators.email('nope'));
    assert.equal(errors['email'], 'Enter a valid email address');
    errors = assignError(errors, 'email', Validators.email('name@example.com'));
    assert.equal(errors['email'], undefined);
    assert.equal(errors['phone'], undefined);
  });

  it('keeps password confirmation in step with the password', () => {
    assert.equal(confirmError('Secret#1', 'Secret#2'), 'Passwords do not match');
    assert.equal(confirmError('Secret#1', 'Secret#1'), '');
  });

  it('rejects a past goal date and clears a corrected one', () => {
    assert.equal(Validators.dateNotPast('2000-01-01', 'Deadline'), 'Deadline cannot be in the past');
    assert.equal(Validators.dateNotPast('2999-01-01', 'Deadline'), '');
  });

  it('rejects an invalid report date range', () => {
    const errors = reportDateErrors('2026-02-10', '2026-02-01', '2026-10-13');
    assert.equal(errors.from, 'Start date must not be after the end date');
    assert.equal(errors.to, 'End date must not be before the start date');
    const fixed = reportDateErrors('2026-02-01', '2026-02-10', '2026-10-13');
    assert.equal(fixed.from, '');
    assert.equal(fixed.to, '');
  });
});

describe('names, reports, and graphs', () => {
  it('joins registration names without sending separate name fields', () => {
    assert.equal(joinFullName(' Vishnu ', ' Kumar ', ' Shrivastav '), 'Vishnu Kumar Shrivastav');
    assert.equal(joinFullName('Vishnu', '  ', 'Shrivastav'), 'Vishnu Shrivastav');
    assert.equal(personName('', 'Middle name', false), '');
    assert.equal(personName('', 'First name', true), 'First name is required');
    assert.deepEqual(splitFullName('Vishnu Kumar Shrivastav'), { firstName: 'Vishnu', middleName: 'Kumar', lastName: 'Shrivastav' });
  });

  it('submits supported report criteria and filters loaded rows', () => {
    assert.deepEqual(reportRequestBody('2026-01-01', '2026-01-31', 'October household review'), {
      fromDate: '2026-01-01',
      toDate: '2026-01-31',
      type: 'October household review',
    });
    const rows = transactionsMatching([
      { id: '1', name: 'Pay', category: 'Salary', type: 'income', amount: 100, date: '2026-01-02' },
      { id: '2', name: 'Grocery shopping', category: 'Food', type: 'expense', amount: 40, date: '2026-01-03' },
      { id: '3', name: 'Old', category: 'Food', type: 'expense', amount: 10, date: '2025-12-01' },
    ], '2026-01-01', '2026-01-31', 'expense', 'Food');
    assert.deepEqual(rows.map((row) => row.id), ['2']);
    assert.deepEqual(totalsFromTransactions(rows), { income: 0, expense: 40, count: 1 });
  });

  it('shows one graph page and stops at the ends', () => {
    const pages = dashboardGraphs({ expenseCount: 1, hasStats: true, savingsCount: 0, budgetCount: 2, goalCount: 0 });
    assert.equal(pages.length, 6);
    assert.equal(pages[0].title, 'Expense breakdown');
    assert.equal(pages[2].hasData, false);
    assert.equal(stepGraph(0, pages.length, -1), 0);
    assert.equal(stepGraph(0, pages.length, 1), 1);
    assert.equal(stepGraph(5, pages.length, 1), 5);
  });

  it('does not invent graph points', () => {
    assert.equal(expenseBreakdown([]), null);
    assert.equal(expenseBreakdown([{ category: 'Salary', type: 'income', amount: 100 }]), null);
    const series = expenseBreakdown([{ category: 'Food', type: 'expense', amount: 40 }]);
    assert.deepEqual(series, { labels: ['Food'], data: [40] });
    const next = expenseBreakdown([
      { category: 'Food', type: 'expense', amount: 40 },
      { category: 'Food', type: 'expense', amount: 20 },
    ]);
    assert.deepEqual(next?.data, [60]);
    assert.equal(incomeExpenseSeries(null), null);
    assert.equal(savingsTrend([]), null);
  });
});

describe('financial sync and approvals', () => {
  it('does not treat a deletion request as a completed delete', async () => {
    const result = transactionDeletionRequest();
    assert.equal(result.deleted, false);
    assert.equal(result.submitted, false);
    assert.equal(result.pending, false);
    assert.equal(API_CAPABILITIES.transactionDeletionApproval, false);
    let refreshed = false;
    const applied = await applyConfirmedDeletion(false, async () => { refreshed = true; });
    assert.equal(applied, false);
    assert.equal(refreshed, false);
    const confirmed = await applyConfirmedDeletion(true, async () => { refreshed = true; });
    assert.equal(confirmed, true);
    assert.equal(refreshed, true);
  });

  it('updates category remaining from confirmed amounts', () => {
    assert.deepEqual(categoryRemaining(10000, 4000), { remaining: 6000, overrun: 0 });
    const after = categoryRemaining(10000, 6000);
    assert.equal(after.remaining, 4000);
    assert.equal(categoryRemaining(10000, 12000).overrun, 2000);
  });

  it('does not add savings to income and does not invent goal progress', () => {
    assert.equal(profileSurplus(50000, 20000), 30000);
    const progress = goalProgress(2500, 10000);
    assert.equal(progress.remaining, 7500);
    assert.equal(progress.percent, 25);
    assert.equal(goalProgress(0, 0).percent, 0);
    assert.equal(goalProgress(50, 40).percent, 100);
  });

  it('keeps a transaction inside the monthly budget', () => {
    const before = monthExpenseTotal([
      { id: '1', type: 'expense', amount: 20000, date: '2026-10-02' },
    ], '2026-10');
    const after = monthExpenseTotalWithDraft([
      { id: '1', type: 'expense', amount: 20000, date: '2026-10-02' },
    ], '2026-10', { type: 'expense', amount: 5000, date: '2026-10-13' });
    const position = monthlyBudgetPosition(30000, after);
    assert.equal(before, 20000);
    assert.equal(after, 25000);
    assert.equal(position.remaining, 5000);
    assert.equal(position.overrun, 0);
    assert.equal(overrunIncrease(30000, before, after), 0);
  });

  it('reports the budget overrun without changing savings', () => {
    const rows = [{ id: '1', type: 'expense', amount: 27000, date: '2026-10-02' }];
    const before = monthExpenseTotal(rows, '2026-10');
    const after = monthExpenseTotalWithDraft(rows, '2026-10', { type: 'expense', amount: 8000, date: '2026-10-13' });
    const position = monthlyBudgetPosition(30000, after);
    assert.equal(after, 35000);
    assert.equal(position.remaining, 0);
    assert.equal(position.overrun, 5000);
    assert.equal(overrunIncrease(30000, before, after), 5000);
    const savings = availableSavings(50000, 20000);
    assert.equal(savings.amount, 30000);
    assert.equal(savings.source, 'derived');
  });

  it('edits an expense once and leaves a pending deletion unchanged', () => {
    const rows = [
      { id: 'old', type: 'expense', amount: 5000, date: '2026-10-02' },
      { id: 'keep', type: 'expense', amount: 20000, date: '2026-10-03' },
    ];
    const edited = monthExpenseTotalWithDraft(rows, '2026-10', { id: 'old', type: 'expense', amount: 8000, date: '2026-10-04' });
    assert.equal(edited, 28000);
    const changedType = monthExpenseTotalWithDraft(rows, '2026-10', { id: 'old', type: 'income', amount: 5000, date: '2026-10-02' });
    assert.equal(changedType, 20000);
    assert.equal(monthExpenseTotal(rows, '2026-10'), 25000);
    assert.equal(monthExpenseTotal(rows.filter((row) => row.id !== 'old'), '2026-10'), 20000);
  });

  it('does not treat total savings as goal progress or spendable cash', () => {
    const progress = goalProgress(20000, 50000);
    assert.equal(progress.percent, 40);
    assert.equal(progress.remaining, 30000);
    assert.notEqual(goalProgress(100000, 50000).percent, progress.percent);
    const goals = [
      { saved: 20000, target: 100000 },
      { saved: 10000, target: 50000 },
    ];
    assert.equal(goalProgress(goals[0].saved, goals[0].target).percent, 20);
    assert.equal(goalProgress(goals[1].saved, goals[1].target).percent, 20);
    const reserved = availableSavings(200000, 50000);
    assert.equal(reserved.amount, 150000);
    const authoritative = availableSavings(200000, 50000, 150000);
    assert.equal(authoritative.amount, 150000);
    assert.equal(authoritative.source, 'backend');
    assert.equal(availableSavings(200000, null).amount, null);
  });

  it('builds one shared position and does not invent an emergency fund', () => {
    const position = buildFinancialPosition({
      profileBudget: 30000,
      categoryLimits: [],
      transactions: [
        { type: 'expense', amount: 27000, date: '2026-10-01' },
        { type: 'income', amount: 9000, date: '2026-10-01' },
        { type: 'expense', amount: 4000, date: '2026-09-01' },
      ],
      savings: [{ amount: 50000, date: '2026-09-01' }, { amount: 1000, date: '2026-10-02' }],
      goals: [{ saved: 20000 }, { saved: 10000 }],
      month: '2026-10',
    });
    assert.equal(position.monthlySpending, 27000);
    assert.equal(position.budgetRemaining, 3000);
    assert.equal(position.totalSavings, 51000);
    assert.equal(position.monthlySavingsChange, 1000);
    assert.equal(position.goalContributionTotal, 30000);
    assert.equal(position.emergencyFund, null);
    assert.equal(position.availableSavings, null);
    assert.equal(API_CAPABILITIES.emergencyFund, false);
    assert.equal(API_CAPABILITIES.affordabilityReservesEmergencyFund, false);
    assert.equal(API_CAPABILITIES.recommendedBudget, false);
  });

  it('does not offer feedback or report deletion in the templates', () => {
    const feedback = readFileSync(new URL('./component/User/feedback/feedback.component.html', root), 'utf8');
    const reports = readFileSync(new URL('./component/Finance/reports/reports.component.html', root), 'utf8');
    const admin = readFileSync(new URL('./component/Admin/admin-feedback/admin-feedback.component.html', root), 'utf8');
    assert.equal(feedback.toLowerCase().includes('delete'), false);
    assert.equal(reports.toLowerCase().includes('delete'), false);
    assert.equal(admin.toLowerCase().includes('delete'), false);
    assert.equal(API_CAPABILITIES.feedbackDelete, false);
    assert.equal(API_CAPABILITIES.reportHistoryDelete, false);
    assert.equal(API_CAPABILITIES.financialSetupApproval, false);
    assert.equal(API_CAPABILITIES.recommendedBudget, false);
  });
});
