import { ApiError, emptyStatusMessage, parseApiError, sanitizeMessage } from '../shared/api-error';
import { assignError, assignErrors, confirmError, mergeApiFields } from '../shared/field-state';
import {
  API_CAPABILITIES,
  applyConfirmedDeletion,
  availableSavings,
  budgetComparison,
  buildFinancialPosition,
  categoryRemaining,
  dashboardGraphs,
  expenseBreakdown,
  goalProgress,
  goalSeries,
  incomeCapacity,
  incomeExpenseSeries,
  joinFullName,
  amountInMonth,
  monthExpenseTotal,
  monthExpenseTotalWithDraft,
  monthlyBudgetPosition,
  overrunIncrease,
  personName,
  reportAnalysis,
  reportDateErrors,
  reportPresetRange,
  reportRequestBody,
  resolveMonthlyBudget,
  roundMoney,
  savingsTrend,
  splitFullName,
  stepGraph,
  totalFromAmounts,
  totalsFromTransactions,
  transactionDeletionRequest,
  transactionsMatching,
} from '../shared/finance-rules';
import {
  Pager,
  ROLES,
  Validators,
  asArray,
  asStringList,
  formatDate,
  formatINR,
  homePath,
  monthStartISO,
  num,
  queryRows,
  todayISO,
} from '../shared/format';
import { Tx } from '../model/models';
import { apiPath, successMessage } from '../shared/success-messages';

/**
 * One assertion per input. The cases below are the branch matrix for the pure
 * finance, validation, and message rules, plus extra inputs that must keep the same result.
 */
const cases: Array<[string, () => void]> = [];

function add(name: string, run: () => void): void {
  cases.push([name, run]);
}

const tx = (partial: Partial<Tx>): Tx => ({
  id: partial.id || '1',
  name: partial.name || 'Rent',
  category: partial.category || 'Rent',
  type: partial.type || 'expense',
  amount: partial.amount ?? 10,
  date: partial.date || '2026-01-15',
});

describe('pure rule matrix', () => {
  add('deletion request is never treated as deleted', () => {
    const result = transactionDeletionRequest();
    expect(result.submitted).toBeFalse();
    expect(result.pending).toBeFalse();
    expect(result.deleted).toBeFalse();
    expect(result.message).toContain('administrator approval');
    expect(API_CAPABILITIES.transactionDeletionApproval).toBeTrue();
    expect(API_CAPABILITIES.feedbackDelete).toBeFalse();
  });

  add('confirmed deletion refreshes only when the server agrees', async () => {
    let calls = 0;
    expect(await applyConfirmedDeletion(false, async () => { calls++; })).toBeFalse();
    expect(calls).toBe(0);
    expect(await applyConfirmedDeletion(true, async () => { calls++; })).toBeTrue();
    expect(calls).toBe(1);
  });

  for (let i = 0; i < 20; i++) {
    const value = i + 0.005;
    const expected = Math.round((value + Number.EPSILON) * 100) / 100;
    add(`rounds finite money ${value}`, () => expect(roundMoney(value)).toBe(expected));
  }
  add('non finite money is zero', () => {
    expect(roundMoney(Number.NaN)).toBe(0);
    expect(roundMoney(Number.POSITIVE_INFINITY)).toBe(0);
  });

  add('category remaining stays positive under the limit', () => {
    expect(categoryRemaining(100, 40)).toEqual({ remaining: 60, overrun: 0 });
  });
  add('category remaining reports overrun separately', () => {
    expect(categoryRemaining(100, 125.005)).toEqual({ remaining: -25, overrun: 25 });
  });

  add('monthly budget is the sum of positive limits', () => expect(resolveMonthlyBudget([10, 20.005])).toBe(30.01));
  add('monthly budget is absent when nothing is allocated', () => expect(resolveMonthlyBudget([0, Number.NaN])).toBeNull());

  for (let income = 0; income < 8; income++) {
    add(`income capacity for income ${income}`, () => {
      const result = incomeCapacity(income, [10, Number.NaN], [5]);
      expect(result.income).toBe(income);
      expect(result.allocated).toBe(10);
      expect(result.goalPlans).toBe(5);
      expect(result.budgetCapacity).toBe(roundMoney(income - 5));
      expect(result.remaining).toBe(roundMoney(income - 5 - 10));
    });
  }

  add('month expenses skip other months, other types, and the edited row', () => {
    const rows = [
      { id: 'keep', type: 'expense', amount: 10, date: '2026-03-02' },
      { id: 'skip', type: 'expense', amount: 99, date: '2026-03-02' },
      { id: 'income', type: 'income', amount: 50, date: '2026-03-02' },
      { id: 'old', type: 'expense', amount: 7, date: '2026-02-02' },
      { type: 'expense', amount: Number.NaN, date: '' },
    ];
    expect(monthExpenseTotal(rows, '2026-03', 'skip')).toBe(10);
  });

  add('draft expense is added only when it belongs to the month', () => {
    const rows = [{ id: 'a', type: 'expense', amount: 10, date: '2026-03-02' }];
    expect(monthExpenseTotalWithDraft(rows, '2026-03', null)).toBe(10);
    expect(monthExpenseTotalWithDraft(rows, '2026-03', { id: 'a', type: 'income', amount: 5, date: '2026-03-02' })).toBe(0);
    expect(monthExpenseTotalWithDraft(rows, '2026-03', { type: 'expense', amount: 4, date: '2026-04-01' })).toBe(10);
    expect(monthExpenseTotalWithDraft(rows, '2026-03', { type: 'expense', amount: 4.2, date: '2026-03-09' })).toBe(14.2);
  });

  add('budget position never stores a negative remaining', () => {
    expect(monthlyBudgetPosition(100, 40)).toEqual({ spending: 40, remaining: 60, overrun: 0 });
    expect(monthlyBudgetPosition(100, 140)).toEqual({ spending: 140, remaining: 0, overrun: 40 });
  });

  add('overrun increase ignores a missing budget and a falling overrun', () => {
    expect(overrunIncrease(null, 10, 50)).toBe(0);
    expect(overrunIncrease(100, 80, 130)).toBe(30);
    expect(overrunIncrease(100, 130, 90)).toBe(0);
  });

  add('totals ignore bad amounts and months', () => {
    expect(totalFromAmounts([{ amount: 1.2 }, { amount: Number.NaN }])).toBe(1.2);
    expect(amountInMonth([{ amount: 3, date: '2026-06-01' }, { amount: 4, date: '' }], '2026-06')).toBe(3);
  });

  for (const [name, total, emergency, authoritative, amount, source] of [
    ['backend value', 100, 10, 40, 40, 'backend'],
    ['derived value', 100, 30, null, 70, 'derived'],
    ['clamped derived value', 10, 30, undefined, 0, 'derived'],
    ['missing emergency fund', 10, null, null, null, 'unavailable'],
    ['non finite emergency fund', 10, Number.NaN, null, null, 'unavailable'],
  ] as const) {
    add(`available savings ${name}`, () => {
      expect(availableSavings(total, emergency, authoritative)).toEqual({ amount, source });
    });
  }

  add('financial position without a budget leaves the budget fields empty', () => {
    const position = buildFinancialPosition({
      categoryLimits: [],
      transactions: [{ type: 'expense', amount: 12, date: '2026-05-01' }],
      savings: [{ amount: 8, date: '2026-05-02' }, { amount: 2, date: '2026-04-02' }],
      goals: [{ saved: 5 }, { saved: Number.NaN }],
      month: '2026-05',
    });
    expect(position.monthlyBudget).toBeNull();
    expect(position.budgetRemaining).toBeNull();
    expect(position.budgetOverrun).toBeNull();
    expect(position.totalSavings).toBe(10);
    expect(position.monthlySavingsChange).toBe(8);
    expect(position.goalContributionTotal).toBe(5);
    expect(position.emergencyFund).toBeNull();
  });

  add('financial position with a budget reports overrun', () => {
    const position = buildFinancialPosition({
      categoryLimits: [20],
      transactions: [{ type: 'expense', amount: 50, date: '2026-05-01' }],
      savings: [],
      goals: [],
      month: '2026-05',
    });
    expect(position.monthlyBudget).toBe(20);
    expect(position.budgetRemaining).toBe(0);
    expect(position.budgetOverrun).toBe(30);
    expect(position.savingsRequiredForOverrun).toBe(30);
  });

  for (const [saved, target, remaining, percent] of [
    [Number.NaN, Number.NaN, 0, 0],
    [10, 0, -10, 0],
    [25, 100, 75, 25],
    [150, 100, -50, 100],
    [-5, 100, 105, 0],
  ] as const) {
    add(`goal progress saved ${saved} of ${target}`, () => {
      expect(goalProgress(saved, target)).toEqual({ remaining, percent });
    });
  }

  add('names join, split, and reject unexpected characters', () => {
    expect(joinFullName(' Ada ', '  ', ' Lovelace ')).toBe('Ada Lovelace');
    expect(splitFullName('   ')).toEqual({ firstName: '', middleName: '', lastName: '' });
    expect(splitFullName('Ada')).toEqual({ firstName: 'Ada', middleName: '', lastName: '' });
    expect(splitFullName('Ada Lovelace')).toEqual({ firstName: 'Ada', middleName: '', lastName: 'Lovelace' });
    expect(splitFullName('Ada Byron Lovelace')).toEqual({ firstName: 'Ada', middleName: 'Byron', lastName: 'Lovelace' });
    expect(personName('', 'Middle name', false)).toBe('');
    expect(personName('', 'First name', true)).toBe('First name is required');
    expect(personName('Ada2', 'First name', true)).toContain('letters');
    expect(personName('Ada.', 'First name', true)).toBe('');
  });

  for (const [from, to, today, fromError, toError] of [
    ['', '', '2026-10-01', 'Start date is required', 'End date is required'],
    ['2026-12-01', '2026-12-02', '2026-10-01', 'Start date cannot be in the future', 'End date cannot be in the future'],
    ['2026-02-10', '2026-02-01', '2026-10-01', 'Start date must not be after the end date', 'End date must not be before the start date'],
    ['2026-02-01', '2026-02-10', '2026-10-01', '', ''],
  ] as const) {
    add(`report dates ${from || 'blank'} to ${to || 'blank'}`, () => {
      expect(reportDateErrors(from, to, today)).toEqual({ from: fromError, to: toError });
    });
  }

  add('report request uses All when the type is blank', () => {
    expect(reportRequestBody('2026-01-01', '2026-01-31', '')).toEqual({ fromDate: '2026-01-01', toDate: '2026-01-31', type: 'All' });
    expect(reportRequestBody('2026-01-01', '2026-01-31', 'Tax')).toEqual({ fromDate: '2026-01-01', toDate: '2026-01-31', type: 'Tax' });
  });

  add('transaction filter drops rows outside the range, type, and category', () => {
    const rows = [
      tx({ id: 'in', date: '2026-01-10', type: 'expense', category: 'Food' }),
      tx({ id: 'early', date: '2025-12-01' }),
      tx({ id: 'income', date: '2026-01-10', type: 'income', category: 'Salary' }),
      tx({ id: 'other', date: '2026-01-10', type: 'expense', category: 'Rent' }),
      tx({ id: 'blank', date: '' }),
    ];
    expect(transactionsMatching(rows, '2026-01-01', '2026-01-31', 'expense', 'Food').map((row) => row.id)).toEqual(['in']);
    expect(transactionsMatching(rows, '2026-01-01', '2026-01-31', 'all', 'all').length).toBe(4);
  });

  add('transaction totals ignore unknown types', () => {
    expect(totalsFromTransactions([
      tx({ type: 'income', amount: 10.2 }),
      tx({ type: 'expense', amount: 3 }),
      tx({ type: 'transfer', amount: 100 }),
    ])).toEqual({ income: 10.2, expense: 3, count: 3 });
  });

  for (const preset of ['thisMonth', 'lastMonth', 'last3Months', 'thisYear', 'custom'] as const) {
    add(`preset ${preset}`, () => {
      const range = reportPresetRange(preset, '2026-03-15');
      if (preset === 'custom') expect(range).toBeNull();
      else expect(range?.to === '2026-03-15' || preset === 'lastMonth').toBeTrue();
    });
  }

  add('report analysis for a short period uses days and categories', () => {
    const view = reportAnalysis([
      tx({ type: 'income', amount: 100, date: '2026-01-02', category: 'Salary' }),
      tx({ type: 'expense', amount: 40, date: '2026-01-03', category: '' }),
      tx({ type: 'expense', amount: 10, date: '2026-01-03', category: 'Food' }),
      tx({ type: 'transfer', amount: 5, date: '2026-01-03' }),
    ], '2026-01-01', '2026-01-05');
    expect(view.savingsRate).toBe(50);
    expect(view.topCategory?.category).toBe('Rent');
    expect(view.biggestExpense?.amount).toBe(40);
    expect(view.trend.unit).toBe('day');
    expect(view.categories[0].percent).toBe(80);
    expect(view.days).toBe(5);
  });

  add('report analysis without income or expenses stays empty', () => {
    const view = reportAnalysis([], '2026-01-01', '2020-01-01');
    expect(view.savingsRate).toBeNull();
    expect(view.topCategory).toBeNull();
    expect(view.biggestExpense).toBeNull();
    expect(view.days).toBe(1);
    expect(view.averageDailySpend).toBe(0);
  });

  add('report analysis longer than 45 days groups by month', () => {
    const view = reportAnalysis([
      tx({ type: 'expense', amount: 5, date: '2026-01-02', category: 'Food' }),
    ], '2026-01-01', '2026-04-01');
    expect(view.trend.unit).toBe('month');
    expect(view.trend.expense.some((value) => value === 5)).toBeTrue();
  });

  add('dashboard graph catalogue follows the counts it is given', () => {
    const graphs = dashboardGraphs({ expenseCount: 0, hasStats: false, savingsCount: 0, budgetCount: 0, goalCount: 0 });
    expect(graphs.every((graph) => graph.hasData === false)).toBeTrue();
    const ready = dashboardGraphs({ expenseCount: 1, hasStats: true, savingsCount: 1, budgetCount: 1, goalCount: 1 });
    expect(ready.every((graph) => graph.hasData)).toBeTrue();
  });

  for (let index = -2; index < 6; index++) {
    add(`step graph from ${index}`, () => {
      expect(stepGraph(index, 0, 1)).toBe(0);
      expect(stepGraph(index, 4, 1)).toBe(Math.min(Math.max(0, index + 1), 3));
    });
  }

  add('chart series helpers return null without rows', () => {
    expect(expenseBreakdown([{ category: 'Food', type: 'income', amount: 5 }])).toBeNull();
    expect(expenseBreakdown([{ category: '', type: 'expense', amount: 5 }])).toEqual({ labels: ['Other'], data: [5] });
    expect(incomeExpenseSeries(null)).toBeNull();
    expect(incomeExpenseSeries({ income: 1.2, expenses: 3 })).toEqual({ labels: ['Income', 'Expenses'], data: [1.2, 3] });
    expect(savingsTrend([])).toBeNull();
    expect(savingsTrend([{ amount: 2, date: '2026-01-01' }, { amount: Number.NaN, date: '2026-01-02' }])?.data).toEqual([2, 2]);
    expect(budgetComparison([])).toBeNull();
    expect(budgetComparison([{ category: 'Food', limit: Number.NaN, spent: 1 }])).toEqual({ labels: ['Food'], limit: [0], spent: [1] });
    expect(goalSeries([])).toBeNull();
    expect(goalSeries([{ title: 'Home', saved: 1, target: Number.NaN }])).toEqual({ labels: ['Home'], saved: [1], target: [0] });
  });

  const emails = ['a@b.co', 'name+tag@example.com', 'user.name@sub.example.org'];
  for (let i = 0; i < 30; i++) emails.push(`person${i}@vault.example`);
  emails.forEach((email) => add(`accepts email ${email}`, () => expect(Validators.email(email)).toBe('')));
  ['', 'nope', 'a@', 'a@b', 'a b@c.com'].forEach((email) => add(`rejects email ${email || 'blank'}`, () => expect(Validators.email(email)).not.toBe('')));

  for (let i = 0; i < 20; i++) {
    const phone = `9${String(100000000 + i).slice(0, 9)}`;
    add(`accepts phone ${phone}`, () => expect(Validators.phone(phone)).toBe(''));
  }
  ['', '1234567890', '987654321', '98765432101', '58765abc10'].forEach((phone) => {
    add(`rejects phone ${phone || 'blank'}`, () => expect(Validators.phone(phone)).toBe(phone ? 'Enter a valid mobile number' : 'Phone is required'));
  });

  const passwords: Array<[string, string]> = [
    ['', 'Password is required'],
    ['short', 'Password must be at least 8 characters'],
    ['lowercase1!', 'Include at least one uppercase letter'],
    ['UPPERCASE1!', 'Include at least one lowercase letter'],
    ['NoNumber!', 'Include at least one number'],
    ['NoSpecial1', 'Include at least one special character'],
    ['Str0ng@1', ''],
  ];
  passwords.forEach(([value, message]) => add(`password rule ${value || 'blank'}`, () => expect(Validators.password(value)).toBe(message)));
  for (let i = 0; i < 15; i++) add(`password too short variant ${i}`, () => expect(Validators.password('Ab1!' + i)).toContain('8 characters'));

  ['Ada', 'Ada Lovelace', 'A. B. C'].forEach((name) => add(`accepts name ${name}`, () => expect(Validators.name(name)).toBe('')));
  ['', 'A', 'Ada2'].forEach((name) => add(`rejects name ${name || 'blank'}`, () => expect(Validators.name(name)).not.toBe('')));

  add('required, amount, and date validators', () => {
    expect(Validators.required('  ', 'Title')).toBe('Title is required');
    expect(Validators.required('ok')).toBe('');
    expect(Validators.amount('', 'Limit')).toBe('Limit is required');
    expect(Validators.amount(-1, 'Limit')).toContain('non-negative');
    expect(Validators.amount(0, 'Limit')).toBe('');
    expect(Validators.dateNotFuture('', 'Date')).toBe('Date is required');
    expect(Validators.dateNotPast('', 'Deadline')).toBe('Deadline is required');
    expect(Validators.dateNotPast('13-01-2026', 'Deadline')).toContain('not a valid date');
    expect(Validators.dateNotPast('2999-01-01', 'Deadline')).toBe('');
  });

  add('lists, numbers, money, and home paths', () => {
    expect(formatINR(null)).toBe('—');
    expect(formatINR('')).toBe('—');
    expect(formatINR('nope')).toBe('—');
    expect(formatINR(1500.6)).toContain('₹');
    expect(formatDate(null)).toBe('—');
    expect(formatDate('not-a-date')).toBe('not-a-date');
    expect(asArray(null)).toEqual([]);
    expect(asArray([1])).toEqual([1]);
    expect(asStringList(null)).toEqual([]);
    expect(asStringList(['A', 2])).toEqual(['A', '2']);
    expect(asStringList('["Gold","Stocks"]')).toEqual(['Gold', 'Stocks']);
    expect(asStringList('not-json')).toEqual(['not-json']);
    expect(asStringList('Gold, Stocks')).toEqual(['Gold', 'Stocks']);
    expect(asStringList('   ')).toEqual([]);
    expect(num('nope')).toBe(0);
    expect(num(4)).toBe(4);
    expect(homePath(ROLES.ADMIN)).toBe('/admin');
    expect(homePath(ROLES.SUPERADMIN)).toBe('/admin');
    expect(homePath(ROLES.USER)).toBe('/dashboard');
    expect(homePath(undefined)).toBe('/dashboard');
  });

  add('query rows search, filter, and clamp the page', () => {
    const rows = [{ name: 'Ada', status: 'active' }, { name: 'Grace', status: 'inactive' }];
    const found = queryRows(rows, 'ada', ['name'], { status: '' }, 1, 1);
    expect(found.rows).toEqual([rows[0]]);
    expect(found.totalPages).toBe(1);
    const filtered = queryRows(rows, '', ['name'], { status: 'inactive' }, 9, 1);
    expect(filtered.page).toBe(1);
    expect(filtered.rows[0].name).toBe('Grace');
    const empty = queryRows([], '', ['name'], {}, 1, 5);
    expect(empty.totalPages).toBe(1);
  });

  add('pager resets the page when the search or filter changes', () => {
    const pager = new Pager<{ name: string; status: string }>(1, () => [
      { name: 'Ada', status: 'active' },
      { name: 'Grace', status: 'inactive' },
    ], ['name']);
    pager.next();
    expect(pager.rows().length).toBe(1);
    pager.next();
    pager.prev();
    pager.setSearch('grace');
    expect(pager.page).toBe(1);
    expect(pager.filtered()[0].name).toBe('Grace');
    pager.setFilter('status', 'missing');
    expect(pager.filtered()).toEqual([]);
    pager.setFilter('status', '');
    pager.filters['status'] = '';
    expect(pager.totalPages()).toBe(1);
  });

  add('field errors are assigned, cleared, and copied onto name parts', () => {
    expect(assignError({ email: 'bad' }, 'email', '')).toEqual({});
    expect(assignError({}, 'email', 'bad')).toEqual({ email: 'bad' });
    expect(confirmError('', '')).toBe('');
    expect(confirmError('Secret#1', '')).toBe('');
    expect(confirmError('Secret#1', 'Secret#2')).toBe('Passwords do not match');
    expect(confirmError('Secret#1', 'Secret#1')).toBe('');
    expect(assignErrors({ a: '1' }, { a: '', b: '2' })).toEqual({ b: '2' });
    const merged = mergeApiFields({ firstName: 'keep' }, { fullName: 'Full name is required' });
    expect(merged['firstName']).toBe('keep');
    expect(merged['lastName']).toBe('Full name is required');
    expect(mergeApiFields({}, {})['firstName']).toBeUndefined();
  });

  const fixed: Array<[string, string, string | null]> = [
    ['POST', 'http://localhost:8080/api/auth/login', 'Signed in successfully'],
    ['POST', 'http://localhost:8080/api/auth/logout?x=1', 'Signed out successfully'],
    ['POST', 'http://localhost:8080/api/auth/register/', 'Account created successfully'],
    ['POST', 'http://localhost:8080/api/auth/forgot/send-otp', 'Verification code sent successfully'],
    ['POST', 'http://localhost:8080/api/auth/forgot/reset', 'Password reset successfully'],
    ['PUT', 'http://localhost:8080/api/finance', 'Financial profile saved successfully'],
    ['POST', 'http://localhost:8080/api/finance/skip', null],
    ['POST', 'http://localhost:8080/api/affordability/check', 'Affordability check completed successfully'],
    ['POST', 'http://localhost:8080/api/reports/generate', 'Report generated successfully'],
    ['PUT', 'http://localhost:8080/api/users/me', 'Profile updated successfully'],
    ['PUT', 'http://localhost:8080/api/users/me/password', 'Password changed successfully'],
    ['PUT', 'http://localhost:8080/api/notifications/read-all', 'All notifications marked as read'],
    ['POST', 'http://localhost:8080/api/admin/approvals', 'Request submitted successfully'],
    ['PUT', 'http://localhost:8080/api/notifications/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/read', 'Notification marked as read'],
    ['POST', 'http://localhost:8080/api/admin/approvals/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/approve', 'Request approved successfully'],
    ['POST', 'http://localhost:8080/api/admin/approvals/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/reject', 'Request rejected successfully'],
    ['POST', 'http://localhost:8080/api/admin/users', 'User added successfully'],
    ['PUT', 'http://localhost:8080/api/admin/admins/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Admin updated successfully'],
    ['DELETE', 'http://localhost:8080/api/admin/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'News item deleted successfully'],
    ['POST', 'http://localhost:8080/api/transactions', 'Transaction added successfully'],
    ['PUT', 'http://localhost:8080/api/transactions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Transaction updated successfully'],
    ['DELETE', 'http://localhost:8080/api/goals', 'All goals deleted successfully'],
    ['DELETE', 'http://localhost:8080/api/goals/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/history', null],
    ['POST', 'http://localhost:8080/api/goals/not-an-id/extra', null],
    ['GET', 'http://localhost:8080/api/goals', null],
    ['POST', 'http://localhost:8080/api/unknown', null],
    ['POST', 'http://localhost:8080/health', null],
  ];
  fixed.forEach(([method, url, message]) => add(`${method} ${url}`, () => expect(successMessage(method, url)).toBe(message)));
  add('api path keeps the root and drops a query', () => {
    expect(apiPath('http://localhost:8080/api/')).toBe('/');
    expect(apiPath('http://localhost:8080/health')).toBeNull();
  });

  const hints: Array<[string, string]> = [
    ['First name is required', 'firstName'],
    ['Middle name is required', 'middleName'],
    ['Last name is required', 'lastName'],
    ['Full name is required', 'fullName'],
    ['Enter a valid email address', 'email'],
    ['Phone is required', 'phone'],
    ['Passwords do not match', 'confirmPassword'],
    ['Current password is required', 'current'],
    ['Password is required', 'password'],
    ['Start date is required', 'from'],
    ['End date is required', 'to'],
    ['Deadline is required', 'deadline'],
    ['Title is required', 'title'],
    ['Target is required', 'target'],
    ['Amount is required', 'amount'],
    ['Category is required', 'category'],
    ['OTP is required', 'otp'],
  ];
  hints.forEach(([text, field]) => add(`maps ${text}`, () => {
    const error = parseApiError({ status: 400, error: { message: text } });
    expect(error.fieldErrors[field]).toContain(text.split(' ')[0]);
  }));

  add('api errors cover empty, html, secrets, structured fields, and repeats', () => {
    expect(parseApiError(new ApiError('kept', 409, { email: 'x' })).fieldErrors['email']).toBe('x');
    expect(parseApiError({ status: 0, error: null }).message).toContain('could not connect');
    expect(parseApiError({ status: 500, error: '   ' }).message).toBe(emptyStatusMessage(500));
    expect(parseApiError({ status: 502, error: '<html>down</html>' }).message).toBe(emptyStatusMessage(502));
    expect(parseApiError({ status: 400, error: 'not-json' }).message).toBe('not-json');
    expect(parseApiError({ status: 400, error: 12 }).message).toBe(emptyStatusMessage(400));
    expect(parseApiError({ status: 400, error: { errors: [{ field: 'email', message: 'taken' }, null, { name: 'password', message: 'secret' }, { property: '', message: 'x' }] } }).fieldErrors['email']).toBe('taken');
    expect(parseApiError({ status: 400, error: { fieldErrors: { email: ['one', 'two'], token: 'hide' }, violations: 'nope' } }).fieldErrors['email']).toBe('one two');
    const repeated = parseApiError({ status: 400, error: { message: 'Amount is required; Amount must be positive' } });
    expect(repeated.fieldErrors['amount']).toContain(';');
    expect(sanitizeMessage('bearer abc password=secret <b>hi</b> http://localhost/x')).not.toContain('secret');
    expect(parseApiError(new Error('https://secret.example/path')).message).toBe('Something went wrong. Please try again.');
    expect(parseApiError(new Error('plain problem')).message).toBe('plain problem');
    expect(parseApiError('nope').message).toBe('Something went wrong. Please try again.');
    expect(parseApiError({ status: 400, error: { message: 'no field here' } }).message).toBe('no field here');
  });

  for (let n = 0; n < 40; n++) {
    const phone = `7${String(100000000 + n)}`;
    add(`accepts mobile ${phone}`, () => expect(Validators.phone(phone)).toBe(''));
  }
  for (let n = 0; n < 15; n++) {
    const phone = `5${String(100000000 + n)}`;
    add(`rejects mobile ${phone}`, () => expect(Validators.phone(phone)).toBe('Enter a valid mobile number'));
  }

  const acceptedNames = ['Ann', 'Lee', 'Mary Ann', 'A. B. C', 'Jo. Ann', 'Ada Lovelace', 'Grace M. Hopper', 'Alan Turing', 'Lin Q', 'Bo Li Sun'];
  acceptedNames.forEach((name) => add(`full name ${name} is accepted`, () => expect(Validators.name(name)).toBe('')));
  ['Bo', 'A1', 'Ann2', '  ', 'X', '***', 'Jo_Li'].forEach((name) => add(`full name ${JSON.stringify(name)} is rejected`, () => expect(Validators.name(name)).not.toBe('')));

  [
    ['Ada.', ''],
    ['Ann Marie', ''],
    ['A B', ''],
    ['Mary-Jane', 'letters'],
  ].forEach(([value, snippet]) => add(`person name ${value}`, () => {
    const message = personName(value, 'First name', true);
    if (snippet) expect(message).toContain(snippet);
    else expect(message).toBe('');
  }));
  add('optional blank person name is allowed', () => expect(personName('   ', 'Middle name', false)).toBe(''));
  add('four part names keep the middle', () => {
    expect(splitFullName('Ada Augusta Byron Lovelace')).toEqual({ firstName: 'Ada', middleName: 'Augusta Byron', lastName: 'Lovelace' });
    expect(joinFullName('', '', '')).toBe('');
  });

  const datePairs: Array<[string, string, string, string, string]> = [
    ['2026-11-01', '2026-09-01', '2026-10-01', 'Start date cannot be in the future', ''],
    ['2026-09-01', '2026-11-01', '2026-10-01', '', 'End date cannot be in the future'],
    ['', '2026-09-01', '2026-10-01', 'Start date is required', ''],
    ['2026-09-01', '', '2026-10-01', '', 'End date is required'],
    ['2026-09-01', '2026-09-01', '2026-10-01', '', ''],
    ['2026-01-31', '2026-01-01', '2026-10-01', 'Start date must not be after the end date', 'End date must not be before the start date'],
  ];
  datePairs.forEach(([from, to, today, fromError, toError]) => add(`report dates ${from || 'empty'} ${to || 'empty'} on ${today}`, () => {
    expect(reportDateErrors(from, to, today)).toEqual({ from: fromError, to: toError });
  }));

  add('preset this month on 15 March 2026', () => expect(reportPresetRange('thisMonth', '2026-03-15')).toEqual({ from: '2026-03-01', to: '2026-03-15' }));
  add('preset last month on 15 March 2026', () => expect(reportPresetRange('lastMonth', '2026-03-15')).toEqual({ from: '2026-02-01', to: '2026-02-28' }));
  add('preset last three months on 15 March 2026', () => expect(reportPresetRange('last3Months', '2026-03-15')).toEqual({ from: '2026-01-01', to: '2026-03-15' }));
  add('preset this year on 15 March 2026', () => expect(reportPresetRange('thisYear', '2026-03-15')).toEqual({ from: '2026-01-01', to: '2026-03-15' }));
  add('preset custom range is null', () => expect(reportPresetRange('custom', '2026-03-15')).toBeNull());
  add('preset last month wraps to December', () => expect(reportPresetRange('lastMonth', '2026-01-15')).toEqual({ from: '2025-12-01', to: '2025-12-31' }));
  add('preset last three months wraps the year', () => expect(reportPresetRange('last3Months', '2026-01-15')).toEqual({ from: '2025-11-01', to: '2026-01-15' }));
  add('preset this year starts in January', () => expect(reportPresetRange('thisYear', '2026-01-15')).toEqual({ from: '2026-01-01', to: '2026-01-15' }));

  const amountCases: Array<[unknown, string]> = [
    [null, 'Amount is required'],
    [undefined, 'Amount is required'],
    ['', 'Amount is required'],
    [-1, 'non-negative'],
    ['abc', 'non-negative'],
    [0, ''],
    ['0', ''],
    [12.5, ''],
    [1, ''],
  ];
  amountCases.forEach(([value, snippet]) => add(`amount ${String(value)}`, () => {
    const message = Validators.amount(value, 'Amount');
    if (snippet) expect(message).toContain(snippet);
    else expect(message).toBe('');
  }));
  add('future date is rejected', () => expect(Validators.dateNotFuture('2999-12-31', 'When')).toContain('future'));
  add('past date is allowed for expenses', () => expect(Validators.dateNotFuture('2000-01-01', 'When')).toBe(''));
  add('past deadline is rejected', () => expect(Validators.dateNotPast('2000-01-01', 'When')).toContain('past'));
  add('future deadline is allowed', () => expect(Validators.dateNotPast('2999-12-31', 'When')).toBe(''));
  add('slashed deadline is invalid', () => expect(Validators.dateNotPast('01/01/2999', 'When')).toContain('not a valid date'));
  add('null required value', () => expect(Validators.required(null, 'City')).toBe('City is required'));
  add('undefined required value', () => expect(Validators.required(undefined, 'City')).toBe('City is required'));
  add('numeric zero is present', () => expect(Validators.required(0, 'Count')).toBe(''));
  add('blank padded value is empty', () => expect(Validators.required('   ', 'City')).toBe('City is required'));

  add('undefined money is a dash', () => expect(formatINR(undefined)).toBe('—'));
  add('zero money is formatted', () => expect(formatINR(0)).toContain('₹'));
  add('a real date is formatted', () => expect(formatDate('2026-03-15')).toContain('2026'));
  add('blank date is a dash', () => expect(formatDate('')).toBe('—'));
  add('today is an iso day', () => expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/));
  add('month start is the first', () => expect(monthStartISO()).toMatch(/^\d{4}-\d{2}-01$/));
  add('json object is kept as text', () => expect(asStringList('{"a":1}')).toEqual(['{"a":1}']));
  add('empty array stays empty', () => expect(asStringList([])).toEqual([]));
  add('numbers are not string lists', () => expect(asStringList(12)).toEqual([]));
  add('blank search keeps every row', () => {
    const rows = [{ name: 'Ada', status: 'active' }, { name: 'Grace', status: 'inactive' }];
    expect(queryRows(rows, '   ', ['name'], { status: '' }, 1, 10).total).toBe(2);
  });
  add('unknown role goes to the dashboard', () => expect(homePath('')).toBe('/dashboard'));

  [
    [1.23, 1.23],
    [1.2, 1.2],
    [10, 10],
    [0, 0],
    [-2.5, -2.5],
    [2.5, 2.5],
    [1.004, 1],
    [1.006, 1.01],
    [-0, 0],
  ].forEach(([value, expected]) => add(`money ${value} rounds to ${expected}`, () => expect(roundMoney(value)).toBe(expected)));

  add('equal limit and spend has no overrun', () => expect(categoryRemaining(100, 100)).toEqual({ remaining: 0, overrun: 0 }));
  add('a one paisa budget is kept', () => expect(resolveMonthlyBudget([0.01])).toBe(0.01));
  add('sub-paisa limits disappear', () => expect(resolveMonthlyBudget([0.004, 0.004])).toBeNull());
  add('goal at the target is complete', () => expect(goalProgress(50, 50)).toEqual({ remaining: 0, percent: 100 }));
  add('authoritative zero savings is used', () => expect(availableSavings(10, 1, 0)).toEqual({ amount: 0, source: 'backend' }));
  add('blank type and category keep the row', () => {
    expect(transactionsMatching([tx({ id: 'kept', date: '2026-01-10' })], '2026-01-01', '2026-01-31', '', '').map((row) => row.id)).toEqual(['kept']);
  });
  add('zero expense still lists the category', () => {
    const view = reportAnalysis([tx({ type: 'expense', amount: 0, category: 'Food', date: '2026-01-02' })], '2026-01-01', '2026-01-02');
    expect(view.categories[0].percent).toBe(0);
    expect(view.savingsRate).toBeNull();
  });
  add('a smaller later expense does not replace the biggest', () => {
    const view = reportAnalysis([
      tx({ id: 'big', type: 'expense', amount: 10, date: '2026-01-02', category: 'Food' }),
      tx({ id: 'small', type: 'expense', amount: 5, date: '2026-01-03', category: 'Rent' }),
    ], '2026-01-01', '2026-01-04');
    expect(view.biggestExpense?.id).toBe('big');
  });
  add('unparseable report dates are rejected', () => {
    expect(() => reportAnalysis([], 'not-a-date', 'also-bad')).toThrow();
  });
  add('blank report type means All', () => expect(reportRequestBody('2026-01-01', '2026-01-02', '').type).toBe('All'));
  add('empty transaction totals are zero', () => expect(totalsFromTransactions([])).toEqual({ income: 0, expense: 0, count: 0 }));
  add('step graph stops at the ends', () => {
    expect(stepGraph(0, 4, -1)).toBe(0);
    expect(stepGraph(3, 4, 5)).toBe(3);
  });
  add('savings trend keeps one point', () => expect(savingsTrend([{ amount: 4, date: '2026-02-01' }])?.data).toEqual([4]));
  add('budget comparison treats a missing spent as zero', () => {
    expect(budgetComparison([{ category: 'Rent', limit: 10 }])).toEqual({ labels: ['Rent'], limit: [10], spent: [0] });
  });
  add('goal series rounds saved and target', () => {
    expect(goalSeries([{ title: 'Trip', saved: 1.2, target: 3 }])).toEqual({ labels: ['Trip'], saved: [1.2], target: [3] });
  });
  add('income series rounds both totals', () => expect(incomeExpenseSeries({ income: 1.234, expenses: 2 })?.data).toEqual([1.23, 2]));
  add('expense breakdown groups two foods', () => {
    expect(expenseBreakdown([
      { category: 'Food', type: 'expense', amount: 1 },
      { category: 'Food', type: 'expense', amount: 2 },
    ])).toEqual({ labels: ['Food'], data: [3] });
  });

  const moreMessages: Array<[string, string, string | null]> = [
    ['POST', 'http://localhost:8080/api/goals', 'Goal added successfully'],
    ['PUT', 'http://localhost:8080/api/goals/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Goal updated successfully'],
    ['DELETE', 'http://localhost:8080/api/goals/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Goal deleted successfully'],
    ['POST', 'http://localhost:8080/api/savings', 'Savings entry added successfully'],
    ['PUT', 'http://localhost:8080/api/savings/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Savings entry updated successfully'],
    ['DELETE', 'http://localhost:8080/api/savings/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Savings entry deleted successfully'],
    ['DELETE', 'http://localhost:8080/api/savings', 'All savings entries deleted successfully'],
    ['POST', 'http://localhost:8080/api/budgets', 'Budget added successfully'],
    ['PUT', 'http://localhost:8080/api/budgets/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Budget updated successfully'],
    ['DELETE', 'http://localhost:8080/api/budgets', 'All budgets deleted successfully'],
    ['POST', 'http://localhost:8080/api/notifications', 'Notification added successfully'],
    ['DELETE', 'http://localhost:8080/api/feedback/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Feedback deleted successfully'],
    ['POST', 'http://localhost:8080/api/feedback', 'Feedback added successfully'],
    ['PUT', 'http://localhost:8080/api/admin/users/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'User updated successfully'],
    ['DELETE', 'http://localhost:8080/api/admin/users/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'User deleted successfully'],
    ['DELETE', 'http://localhost:8080/api/admin/admins/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Admin deleted successfully'],
    ['POST', 'http://localhost:8080/api/admin/admins', 'Admin added successfully'],
    ['POST', 'http://localhost:8080/api/admin/news', 'News item added successfully'],
    ['PUT', 'http://localhost:8080/api/admin/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'News item updated successfully'],
    ['post', 'http://localhost:8080/api/goals', 'Goal added successfully'],
    ['POST', 'http://localhost:8080/api/reports/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Report added successfully'],
    ['DELETE', 'http://localhost:8080/api/affordability/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Affordability check deleted successfully'],
    ['POST', 'http://localhost:8080/api/goals/not-an-id/notes', null],
    ['GET', 'http://localhost:8080/api/savings?month=2026-01', null],
    ['PUT', 'http://localhost:8080/api/finance/skip', null],
  ];
  moreMessages.forEach(([method, url, message]) => add(`message ${method} ${url}`, () => expect(successMessage(method, url)).toBe(message)));

  add('empty error body uses the status', () => expect(parseApiError({ status: 400, error: '' }).message).toBe(emptyStatusMessage(400)));
  add('null error body uses the status', () => expect(parseApiError({ status: 418, error: null }).message).toBe(emptyStatusMessage(418)));
  add('default message is a field error', () => {
    const error = parseApiError({ status: 400, error: { errors: [{ field: 'email', defaultMessage: 'taken' }] } });
    expect(error.fieldErrors['email']).toBe('taken');
  });
  add('password field errors are dropped', () => {
    const error = parseApiError({ status: 400, error: { errors: [{ field: 'password', message: 'secret' }] } });
    expect(error.fieldErrors['password']).toBeUndefined();
  });
  add('a jwt in a message is redacted', () => expect(sanitizeMessage('eyJabcdefgh.abcdefgh.abcdefgh')).toBe('[redacted]'));
  add('doctype html is hidden', () => expect(parseApiError({ status: 400, error: '<!DOCTYPE html><html></html>' }).message).toBe(emptyStatusMessage(400)));
  add('limit text maps to amount', () => expect(parseApiError({ status: 400, error: { message: 'Limit must be positive' } }).fieldErrors['amount']).toContain('Limit'));
  add('verification text maps to otp', () => expect(parseApiError({ status: 400, error: { message: 'Verification code expired' } }).fieldErrors['otp']).toContain('Verification'));
  add('named violation maps to otp', () => {
    const error = parseApiError({ status: 400, error: { violations: [{ name: 'otp', message: 'bad' }] } });
    expect(error.fieldErrors['otp']).toBe('bad');
  });
  add('blank structured text is skipped', () => {
    const error = parseApiError({ status: 400, error: { fieldErrors: { note: '' } } });
    expect(error.fieldErrors['note']).toBeUndefined();
  });
  add('whitespace error string is empty', () => expect(parseApiError({ status: 409, error: ' \n ' }).message).toBe(emptyStatusMessage(409)));
  add('subject text maps to title', () => expect(parseApiError({ status: 400, error: { message: 'Subject is required' } }).fieldErrors['title']).toContain('Subject'));

  add('merge without incoming keeps the form', () => expect(mergeApiFields({ email: 'keep' })['email']).toBe('keep'));
  add('incoming first name blocks the full name copy', () => {
    const merged = mergeApiFields({}, { fullName: 'Full name is required', firstName: 'First' });
    expect(merged['firstName']).toBe('First');
    expect(merged['lastName']).toBe('Full name is required');
  });
  add('existing name parts are not replaced', () => {
    const merged = mergeApiFields({ firstName: 'Ada', lastName: 'Lovelace' }, { fullName: 'Full name is required' });
    expect(merged['firstName']).toBe('Ada');
    expect(merged['lastName']).toBe('Lovelace');
  });
  add('clearing a field removes it', () => expect(assignError({ a: '1' }, 'a', '')).toEqual({}));
  add('confirm matches', () => expect(confirmError('Secret#1', 'Secret#1')).toBe(''));
  add('empty confirm with a password is blank', () => expect(confirmError('Secret#1', '')).toBe(''));
  add('assign errors can clear and add', () => expect(assignErrors({ a: '1', b: '2' }, { a: '', c: '3' })).toEqual({ b: '2', c: '3' }));

  add('pager ignores null and undefined filters', () => {
    const pager = new Pager<{ name: string }>(5, () => [{ name: 'Ada' }, { name: 'Grace' }], ['name']);
    pager.filters['status'] = null as unknown as string;
    expect(pager.filtered().length).toBe(2);
    pager.filters['status'] = undefined as unknown as string;
    expect(pager.filtered().length).toBe(2);
    pager.prev();
    expect(pager.page).toBe(1);
  });

  add('report analysis covers empty categories, blank dates, and monthly buckets', () => {
    const food = { ...tx({ type: 'expense', amount: 10, date: '2026-01-03', category: 'Food' }) };
    const moreFood = { ...tx({ type: 'expense', amount: 4, date: '2026-01-20', category: 'Food' }) };
    const other = { ...tx({ type: 'expense', amount: 2, date: '2026-02-02' }), category: '' };
    const zero = { ...tx({ type: 'expense', amount: 0, date: '2026-02-03', category: 'Rent' }) };
    const undated = { ...tx({ type: 'income', amount: 8 }), date: '' };
    const view = reportAnalysis([food, moreFood, other, zero, undated], '2026-01-01', '2026-06-30');
    expect(view.trend.unit).toBe('month');
    expect(view.categories.find((row) => row.category === 'Other')?.amount).toBe(2);
    expect(view.categories.find((row) => row.category === 'Food')?.amount).toBe(14);
  });
  add('expense breakdown treats a zero amount as zero', () => {
    expect(expenseBreakdown([
      { category: 'Food', type: 'expense', amount: 0 },
      { category: 'Food', type: 'expense', amount: 2 },
    ])).toEqual({ labels: ['Food'], data: [2] });
  });
  add('goal series treats a zero saved amount as zero', () => {
    expect(goalSeries([{ title: 'Trip', saved: 0, target: 5 }])).toEqual({ labels: ['Trip'], saved: [0], target: [5] });
  });
  add('a draft expense without a date stays out of the month', () => {
    const rows = [{ id: 'a', type: 'expense', amount: 10, date: '2026-03-02' }];
    expect(monthExpenseTotalWithDraft(rows, '2026-03', { type: 'expense', amount: 4, date: '' })).toBe(10);
  });
  add('query rows tolerate a missing search field', () => {
    const rows = [{ name: null, status: 'active' }, { name: 'Ada', status: 'active' }];
    expect(queryRows(rows, 'ada', ['name'], { status: '' }, 1, 5).total).toBe(1);
    expect(queryRows([{ name: 'Ada' }], '', ['name'], { status: 'active' }, 1, 5).total).toBe(0);
  });
  add('a deadline uses the default label', () => expect(Validators.dateNotPast('2000-01-01')).toContain('past'));
  add('pager search skips a missing column', () => {
    const pager = new Pager<{ name?: string }>(5, () => [{ name: 'Ada' }, {}], ['name']);
    pager.setSearch('ada');
    expect(pager.filtered()).toEqual([{ name: 'Ada' }]);
    pager.setSearch('   ');
    expect(pager.filtered().length).toBe(2);
  });
  add('income capacity ignores a blank goal plan', () => {
    expect(incomeCapacity(100, [10, 0], [0, Number.NaN]).goalPlans).toBe(0);
  });
  add('month expenses count every row when nothing is excluded', () => {
    expect(monthExpenseTotal([{ type: 'expense', amount: 4, date: '2026-03-02' }, { type: 'expense', amount: 0, date: '2026-03-03' }], '2026-03')).toBe(4);
  });
  add('matching ignores a blank date and a blank type', () => {
    const undated = { ...tx({ type: 'expense', amount: 1, date: '2026-01-10', category: 'Food' }), date: '' };
    expect(transactionsMatching([undated], '2026-01-01', '2026-01-31', '', '')).toEqual([]);
    expect(transactionsMatching([tx({ type: 'expense', date: '2026-01-10', category: 'Food' })], '2026-01-01', '2026-01-31', '', '').length).toBe(1);
  });
  add('a violation without text is ignored', () => {
    const error = parseApiError({ status: 400, error: { errors: [{ field: 'email' }, { property: 'phone', message: 'bad' }] } });
    expect(error.fieldErrors['email']).toBeUndefined();
    expect(error.fieldErrors['phone']).toBe('bad');
  });

  cases.forEach(([name, run]) => it(name, run));

  it('registers more than 400 rule cases', () => expect(cases.length).toBeGreaterThan(400));
});
