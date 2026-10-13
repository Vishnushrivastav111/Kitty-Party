import { Tx } from '../model/models';

export const API_CAPABILITIES = {
  transactionDeletionApproval: false,
  financialSetupApproval: false,
  reportCategoryFilter: false,
  reportTypeFilter: false,
  planningRecommendations: false,
  recommendedBudget: false,
  feedbackDelete: false,
  reportHistoryDelete: false,
} as const;

export const UNSUPPORTED_DELETION_MESSAGE =
  'This transaction was not deleted. Deletion requires administrator approval, and the API has no operation to submit or review that request.';

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

export function goalProgress(saved: number, target: number): { remaining: number; percent: number } {
  const safeSaved = Number.isFinite(saved) ? saved : 0;
  const safeTarget = Number.isFinite(target) ? target : 0;
  if (!(safeTarget > 0)) return { remaining: roundMoney(safeTarget - safeSaved), percent: 0 };
  const percent = Math.min(100, Math.max(0, (safeSaved / safeTarget) * 100));
  return { remaining: roundMoney(safeTarget - safeSaved), percent: Math.round(percent * 100) / 100 };
}

/** Matches the finance service summary: surplus is income minus expenses, floored at zero. EMI is stored separately and is not subtracted. */
export function profileSurplus(income: number, expenses: number): number {
  return Math.max(0, roundMoney(income - expenses));
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
