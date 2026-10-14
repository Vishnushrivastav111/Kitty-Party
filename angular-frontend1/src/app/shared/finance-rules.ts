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

export const UNSUPPORTED_DELETION_MESSAGE = '';

export interface DeletionRequestResult {
  submitted: boolean;
  pending: boolean;
  deleted: boolean;
  message: string;
}

export async function applyConfirmedDeletion(confirmedByServer: boolean, refresh: () => Promise<void>): Promise<boolean> {
  if (!confirmedByServer) return false;
  await refresh();
  return true;
}

export function transactionDeletionRequest(): DeletionRequestResult {
  return { submitted: false, pending: false, deleted: false, message: '' };
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

export function resolveMonthlyBudget(profileBudget: number | null | undefined, categoryLimits: number[]): number | null {
  const profile = Number(profileBudget);
  if (Number.isFinite(profile) && profile > 0) return roundMoney(profile);
  const limits = categoryLimits.reduce((sum, value) => roundMoney(sum + (Number(value) || 0)), 0);
  return limits > 0 ? limits : null;
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

export interface BudgetPosition { spending: number; remaining: number; overrun: number; }
export function monthlyBudgetPosition(budget: number, spending: number): BudgetPosition {
  const gap = categoryRemaining(budget, spending);
  return { spending: roundMoney(spending), remaining: Math.max(0, gap.remaining), overrun: gap.overrun };
}

export function overrunIncrease(budget: number | null | undefined, before: number, after: number): number {
  if (!Number.isFinite(Number(budget)) || Number(budget) <= 0) return 0;
  return roundMoney(Math.max(0, after - Number(budget)) - Math.max(0, before - Number(budget)));
}
