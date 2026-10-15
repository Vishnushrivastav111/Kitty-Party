import { Tx } from '../model/models';

export const API_CAPABILITIES = {
  transactionDeletionApproval: true,
  financialSetupApproval: false,
  reportCategoryFilter: false,
  reportTypeFilter: false,
  planningRecommendations: false,
  recommendedBudget: false,
  emergencyFund: false,
  affordabilityReservesEmergencyFund: false,
  feedbackDelete: false,
  reportHistoryDelete: false,
} as const;

export const UNSUPPORTED_DELETION_MESSAGE =
  'This transaction was not deleted. Deleting it needs administrator approval.';

export interface DeletionRequestResult {
  submitted: false;
  pending: false;
  deleted: false;
  message: string;
}

export function transactionDeletionRequest(): DeletionRequestResult {
  return {
    submitted: false,
    pending: false,
    deleted: false,
    message: UNSUPPORTED_DELETION_MESSAGE,
  };
}

export async function applyConfirmedDeletion(confirmedByServer: boolean, refresh: () => Promise<void>): Promise<boolean> {
  if (!confirmedByServer) return false;
  await refresh();
  return true;
}

export function roundMoney(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function categoryRemaining(limit: number, spent: number): { remaining: number; overrun: number } {
  const remaining = roundMoney(limit - spent);
  return { remaining, overrun: remaining < 0 ? roundMoney(-remaining) : 0 };
}

export interface MoneyRow {
  id?: string;
  type?: string;
  amount: number;
  date: string;
  category?: string;
}

/** The monthly budget is the sum of the saved category limits. Never substitutes a default. */
export function resolveMonthlyBudget(categoryLimits: number[]): number | null {
  const limits = categoryLimits.reduce((sum, value) => roundMoney(sum + (Number(value) || 0)), 0);
  return limits > 0 ? limits : null;
}

export interface IncomeCapacity {
  /** Take-home plus other recurring income. */
  income: number;
  /** Sum of category budget limits. */
  allocated: number;
  /** Sum of planned monthly contributions of active goals. */
  goalPlans: number;
  /** Income minus active goal contributions: the most category budgets can add up to. */
  budgetCapacity: number;
  /** Budget capacity minus category budgets. Negative means over-committed. */
  remaining: number;
}

/**
 * Income-only capacity: category budgets and active goal plans are the commitments.
 * Matches the server summary (FinancialPlan.java). Expenses and EMI are not entered in setup.
 */
export function incomeCapacity(income: number, categoryLimits: number[], goalPlans: number[]): IncomeCapacity {
  const total = roundMoney(Number(income) || 0);
  const allocated = categoryLimits.reduce((sum, value) => roundMoney(sum + (Number(value) || 0)), 0);
  const plans = goalPlans.reduce((sum, value) => roundMoney(sum + (Number(value) || 0)), 0);
  const budgetCapacity = roundMoney(total - plans);
  return { income: total, allocated, goalPlans: plans, budgetCapacity, remaining: roundMoney(budgetCapacity - allocated) };
}

export function monthExpenseTotal(rows: MoneyRow[], month: string, excludeId?: string): number {
  let total = 0;
  for (const row of rows) {
    if (excludeId && row.id === excludeId) continue;
    if (row.type !== 'expense') continue;
    if (String(row.date || '').slice(0, 7) !== month) continue;
    total = roundMoney(total + (Number(row.amount) || 0));
  }
  return total;
}

export function monthExpenseTotalWithDraft(rows: MoneyRow[], month: string, draft: MoneyRow | null): number {
  const base = monthExpenseTotal(rows, month, draft?.id);
  if (!draft || draft.type !== 'expense' || String(draft.date || '').slice(0, 7) !== month) return base;
  return roundMoney(base + (Number(draft.amount) || 0));
}

export interface BudgetPosition {
  spending: number;
  remaining: number;
  overrun: number;
}

/** Remaining is never negative. The excess is reported separately as overrun. */
export function monthlyBudgetPosition(budget: number, spending: number): BudgetPosition {
  const gap = categoryRemaining(budget, spending);
  return {
    spending: roundMoney(spending),
    remaining: Math.max(0, gap.remaining),
    overrun: gap.overrun,
  };
}

/** How much of a new or edited expense falls outside the monthly budget. Does not change savings. */
export function overrunIncrease(budget: number | null, spendingBefore: number, spendingAfter: number): number {
  if (budget == null) return 0;
  const before = monthlyBudgetPosition(budget, spendingBefore).overrun;
  const after = monthlyBudgetPosition(budget, spendingAfter).overrun;
  return Math.max(0, roundMoney(after - before));
}

export function totalFromAmounts(rows: Array<{ amount: number }>): number {
  return rows.reduce((sum, row) => roundMoney(sum + (Number(row.amount) || 0)), 0);
}

export function amountInMonth(rows: Array<{ amount: number; date: string }>, month: string): number {
  return totalFromAmounts(rows.filter((row) => String(row.date || '').slice(0, 7) === month));
}

/**
 * Uses an authoritative available-savings figure when the API sends one.
 * Otherwise subtracts a known emergency fund once. Missing emergency data stays unavailable.
 */
export function availableSavings(
  totalSavings: number,
  emergencyFund: number | null,
  authoritative?: number | null,
): { amount: number | null; source: 'backend' | 'derived' | 'unavailable' } {
  if (authoritative != null && Number.isFinite(Number(authoritative))) {
    return { amount: roundMoney(Number(authoritative)), source: 'backend' };
  }
  if (emergencyFund == null || !Number.isFinite(Number(emergencyFund))) {
    return { amount: null, source: 'unavailable' };
  }
  return { amount: Math.max(0, roundMoney(totalSavings - Number(emergencyFund))), source: 'derived' };
}

export interface FinancialPosition {
  month: string;
  monthlyBudget: number | null;
  monthlySpending: number;
  budgetRemaining: number | null;
  budgetOverrun: number | null;
  totalSavings: number;
  monthlySavingsChange: number;
  emergencyFund: null;
  availableSavings: null;
  savingsRequiredForOverrun: number | null;
  goalContributionTotal: number;
}

export function buildFinancialPosition(input: {
  categoryLimits: number[];
  transactions: MoneyRow[];
  savings: Array<{ amount: number; date: string }>;
  goals: Array<{ saved: number }>;
  month: string;
}): FinancialPosition {
  const monthlyBudget = resolveMonthlyBudget(input.categoryLimits);
  const monthlySpending = monthExpenseTotal(input.transactions, input.month);
  const budget = monthlyBudget == null ? null : monthlyBudgetPosition(monthlyBudget, monthlySpending);
  const totalSavings = totalFromAmounts(input.savings);
  return {
    month: input.month,
    monthlyBudget,
    monthlySpending,
    budgetRemaining: budget ? budget.remaining : null,
    budgetOverrun: budget ? budget.overrun : null,
    totalSavings,
    monthlySavingsChange: amountInMonth(input.savings, input.month),
    emergencyFund: null,
    availableSavings: null,
    savingsRequiredForOverrun: budget ? budget.overrun : null,
    goalContributionTotal: input.goals.reduce((sum, goal) => roundMoney(sum + (Number(goal.saved) || 0)), 0),
  };
}

export function goalProgress(saved: number, target: number): { remaining: number; percent: number } {
  const safeSaved = Number.isFinite(saved) ? saved : 0;
  const safeTarget = Number.isFinite(target) ? target : 0;
  if (!(safeTarget > 0)) return { remaining: roundMoney(safeTarget - safeSaved), percent: 0 };
  const percent = Math.min(100, Math.max(0, (safeSaved / safeTarget) * 100));
  return { remaining: roundMoney(safeTarget - safeSaved), percent: Math.round(percent * 100) / 100 };
}

export function joinFullName(firstName: string, middleName: string, lastName: string): string {
  return [firstName, middleName, lastName].map((part) => part.trim()).filter(Boolean).join(' ');
}

export function splitFullName(fullName: string): { firstName: string; middleName: string; lastName: string } {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return { firstName: '', middleName: '', lastName: '' };
  if (parts.length === 1) return { firstName: parts[0], middleName: '', lastName: '' };
  if (parts.length === 2) return { firstName: parts[0], middleName: '', lastName: parts[1] };
  return { firstName: parts[0], middleName: parts.slice(1, -1).join(' '), lastName: parts[parts.length - 1] };
}

export function personName(value: string, label: string, required: boolean): string {
  const text = value.trim();
  if (!text) return required ? `${label} is required` : '';
  if (!/^[A-Za-z][A-Za-z .]*$/.test(text)) return `${label} can only contain letters, spaces, and periods`;
  return '';
}

export function reportDateErrors(from: string, to: string, today: string): { from: string; to: string } {
  let fromError = !from ? 'Start date is required' : from > today ? 'Start date cannot be in the future' : '';
  let toError = !to ? 'End date is required' : to > today ? 'End date cannot be in the future' : '';
  if (!fromError && !toError && from > to) {
    fromError = 'Start date must not be after the end date';
    toError = 'End date must not be before the start date';
  }
  return { from: fromError, to: toError };
}

export function reportRequestBody(fromDate: string, toDate: string, type: string): { fromDate: string; toDate: string; type: string } {
  return { fromDate, toDate, type: type || 'All' };
}

export function transactionsMatching(rows: Tx[], from: string, to: string, type: string, category: string): Tx[] {
  return rows.filter((row) => {
    const date = String(row.date || '').slice(0, 10);
    if (!date || date < from || date > to) return false;
    if (type && type !== 'all' && row.type !== type) return false;
    if (category && category !== 'all' && row.category !== category) return false;
    return true;
  });
}

export function totalsFromTransactions(rows: Tx[]): { income: number; expense: number; count: number } {
  let income = 0;
  let expense = 0;
  for (const row of rows) {
    const amount = roundMoney(Number(row.amount) || 0);
    if (row.type === 'income') income = roundMoney(income + amount);
    else if (row.type === 'expense') expense = roundMoney(expense + amount);
  }
  return { income, expense, count: rows.length };
}

export type ReportPreset = 'thisMonth' | 'lastMonth' | 'last3Months' | 'thisYear' | 'custom';

function isoDay(year: number, monthIndex: number, day: number): string {
  return new Date(Date.UTC(year, monthIndex, day)).toISOString().slice(0, 10);
}

/** Start and end dates for a quick period, or null for a custom range. */
export function reportPresetRange(preset: ReportPreset, today: string): { from: string; to: string } | null {
  const [year, month] = today.split('-').map(Number);
  const monthIndex = month - 1;
  if (preset === 'thisMonth') return { from: isoDay(year, monthIndex, 1), to: today };
  if (preset === 'lastMonth') return { from: isoDay(year, monthIndex - 1, 1), to: isoDay(year, monthIndex, 0) };
  if (preset === 'last3Months') return { from: isoDay(year, monthIndex - 2, 1), to: today };
  if (preset === 'thisYear') return { from: isoDay(year, 0, 1), to: today };
  return null;
}

function daysBetween(from: string, to: string): number {
  const a = Date.parse(`${from}T00:00:00Z`);
  const b = Date.parse(`${to}T00:00:00Z`);
  if (!Number.isFinite(a) || !Number.isFinite(b) || b < a) return 1;
  return Math.round((b - a) / 86400000) + 1;
}

export interface ReportAnalysis {
  income: number;
  expense: number;
  net: number;
  /** Share of income that was not spent, in percent. Null when there is no income. */
  savingsRate: number | null;
  count: number;
  days: number;
  averageDailySpend: number;
  categories: Array<{ category: string; amount: number; percent: number }>;
  topCategory: { category: string; amount: number } | null;
  biggestExpense: Tx | null;
  trend: { unit: 'day' | 'month'; labels: string[]; income: number[]; expense: number[] };
}

/** Everything the Reports page shows for one period, calculated from the matching transactions. */
export function reportAnalysis(rows: Tx[], from: string, to: string): ReportAnalysis {
  const totals = totalsFromTransactions(rows);
  const days = daysBetween(from, to);
  const net = roundMoney(totals.income - totals.expense);
  const byCategory: Record<string, number> = {};
  let biggest: Tx | null = null;
  for (const row of rows) {
    if (row.type !== 'expense') continue;
    const amount = roundMoney(Number(row.amount) || 0);
    const key = row.category || 'Other';
    byCategory[key] = roundMoney((byCategory[key] || 0) + amount);
    if (!biggest || amount > Number(biggest.amount)) biggest = row;
  }
  const categories = Object.keys(byCategory)
    .map((category) => ({
      category,
      amount: byCategory[category],
      percent: totals.expense > 0 ? Math.round((byCategory[category] / totals.expense) * 1000) / 10 : 0,
    }))
    .sort((a, b) => b.amount - a.amount);

  const byDay = days <= 45;
  const buckets = new Map<string, { income: number; expense: number }>();
  if (byDay) {
    for (let i = 0; i < days; i++) {
      const [y, m, d] = from.split('-').map(Number);
      buckets.set(isoDay(y, m - 1, d + i), { income: 0, expense: 0 });
    }
  }
  for (const row of rows) {
    const key = String(row.date || '').slice(0, byDay ? 10 : 7);
    if (!key || (row.type !== 'income' && row.type !== 'expense')) continue;
    const kind = row.type as 'income' | 'expense';
    const bucket = buckets.get(key) || { income: 0, expense: 0 };
    bucket[kind] = roundMoney(bucket[kind] + (Number(row.amount) || 0));
    buckets.set(key, bucket);
  }
  const labels = [...buckets.keys()].sort();
  return {
    income: totals.income,
    expense: totals.expense,
    net,
    savingsRate: totals.income > 0 ? Math.round((net / totals.income) * 1000) / 10 : null,
    count: totals.count,
    days,
    averageDailySpend: roundMoney(totals.expense / days),
    categories,
    topCategory: categories.length ? { category: categories[0].category, amount: categories[0].amount } : null,
    biggestExpense: biggest,
    trend: {
      unit: byDay ? 'day' : 'month',
      labels,
      income: labels.map((label) => buckets.get(label)!.income),
      expense: labels.map((label) => buckets.get(label)!.expense),
    },
  };
}

export interface GraphSpec {
  id: string;
  title: string;
  hasData: boolean;
  empty: string;
}

export function dashboardGraphs(input: {
  expenseCount: number;
  hasStats: boolean;
  savingsCount: number;
  budgetCount: number;
  goalCount: number;
}): GraphSpec[] {
  return [
    { id: 'expense', title: 'Expense breakdown', hasData: input.expenseCount > 0, empty: 'No expense transactions are available for this graph.' },
    { id: 'income', title: 'Income vs expense', hasData: input.hasStats, empty: 'Income and expense totals are not available yet.' },
    { id: 'savings', title: 'Savings trend', hasData: input.savingsCount > 0, empty: 'No savings entries are available for this graph.' },
    { id: 'budget', title: 'Monthly budget comparison', hasData: input.budgetCount > 0, empty: 'No category budgets are available for this graph.' },
    { id: 'health', title: 'Financial health', hasData: input.hasStats, empty: 'A financial health score is not available yet.' },
    { id: 'goals', title: 'Goal progress', hasData: input.goalCount > 0, empty: 'No goals are available for this graph.' },
  ];
}

export function stepGraph(index: number, total: number, delta: number): number {
  if (total <= 0) return 0;
  return Math.min(Math.max(0, index + delta), total - 1);
}

export interface Series {
  labels: string[];
  data: number[];
}

export function expenseBreakdown(rows: Array<{ category: string; type: string; amount: number }>): Series | null {
  const totals: Record<string, number> = {};
  for (const row of rows) {
    if (row.type !== 'expense') continue;
    totals[row.category || 'Other'] = roundMoney((totals[row.category || 'Other'] || 0) + Number(row.amount || 0));
  }
  const labels = Object.keys(totals);
  if (!labels.length) return null;
  return { labels, data: labels.map((label) => totals[label]) };
}

export function incomeExpenseSeries(stats: { income: number; expenses: number } | null): Series | null {
  if (!stats) return null;
  return { labels: ['Income', 'Expenses'], data: [roundMoney(stats.income), roundMoney(stats.expenses)] };
}

export function savingsTrend(rows: Array<{ amount: number; date: string }>): Series | null {
  if (!rows.length) return null;
  let running = 0;
  const labels: string[] = [];
  const data: number[] = [];
  for (const row of rows) {
    running = roundMoney(running + Number(row.amount || 0));
    labels.push(row.date);
    data.push(running);
  }
  return { labels, data };
}

export function budgetComparison(rows: Array<{ category: string; limit: number; spent?: number }>): { labels: string[]; limit: number[]; spent: number[] } | null {
  if (!rows.length) return null;
  return {
    labels: rows.map((row) => row.category),
    limit: rows.map((row) => roundMoney(Number(row.limit) || 0)),
    spent: rows.map((row) => roundMoney(Number(row.spent) || 0)),
  };
}

export function goalSeries(rows: Array<{ title: string; saved: number; target: number }>): { labels: string[]; saved: number[]; target: number[] } | null {
  if (!rows.length) return null;
  return {
    labels: rows.map((row) => row.title),
    saved: rows.map((row) => roundMoney(Number(row.saved) || 0)),
    target: rows.map((row) => roundMoney(Number(row.target) || 0)),
  };
}
