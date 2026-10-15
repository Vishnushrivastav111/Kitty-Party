export interface VaultUser {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  role: string;
  status?: string;
  createdAt?: string;
  setupComplete?: boolean;
}

export interface FinanceProfile {
  id?: string;
  userId?: string;
  incomeSource?: string;
  monthlyIncome?: number;
  additionalIncome?: number;
  emergencyReserve?: number;
  payCycle?: string;
  /**
   * Retired: expenses, loans/EMI, commitments, monthly budget and budget style are no longer
   * collected in setup (category budgets own them). Old rows may still return these values;
   * nothing in the app reads them.
   */
  otherCommitments?: number;
  monthlyExpenses?: number;
  expenseCategories?: unknown;
  hasLoan?: unknown;
  loanType?: string;
  loanAmount?: number;
  monthlyEmi?: number;
  loansEmi?: number;
  emiStartDate?: string;
  currentSavings?: number;
  savingsType?: string;
  investments?: number;
  investmentTypes?: unknown;
  monthlyBudget?: number;
  budgetStyle?: string;
  setupDate?: string;
}

export interface DashStats {
  fullName?: string;
  health: number;
  totalSavings: number;
  monthBudget: number;
  spent: number;
  activeGoals: number;
  income: number;
  expenses: number;
}

export interface Tx {
  id: string;
  name: string;
  category: string;
  type: string;
  amount: number;
  date: string;
  note?: string;
}

export interface Goal {
  id: string;
  title: string;
  category: string;
  target: number;
  saved: number;
  deadline?: string;
  status: string;
  description?: string;
  priority?: string;
  plannedContribution?: number;
  requiredMonthly?: number;
  feasible?: boolean;
  feasibilityMessage?: string;
  progressPercent?: number;
  remaining?: number;
}

export interface Saving {
  id: string;
  title: string;
  category: string;
  amount: number;
  date: string;
  note?: string;
  goalId?: string;
}

export interface Budget {
  id: string;
  category: string;
  limit: number;
  note?: string;
  spent?: number;
  remaining?: number;
  usedPercent?: number;
  usage?: string;
  status?: string;
}

export interface Report {
  id: string;
  type: string;
  fromDate: string;
  toDate: string;
  income: number;
  expense: number;
  net: number;
  txCount: number;
  createdAt?: string;
}

export interface AffordCheck {
  id: string;
  itemName: string;
  amount: number;
  available?: number;
  availableAmount?: number;
  verdict: string;
  level: string;
  priority?: string;
  date?: string;
  suggestion?: string;
  plan?: string[];
  shortfall?: number;
  monthsToSave?: number;
  paymentMethod?: string;
  monthlyInstallment?: number;
  totalRepayment?: number;
  canCreateGoal?: boolean;
  /** Savings that were free to use for this check, excluding the emergency reserve and goal money. */
  unallocatedLiquid?: number;
  emergencyReserve?: number;
  /** Savings kept back for needs (3 months of income). Only set for wants. */
  protectedSavings?: number;
}

/** The complete savings picture, calculated once on the server. */
export interface SavingsOverview {
  allocatedSavings: number;
  unallocatedLiquid: number;
  emergencyReserve: number;
  totalSavings: number;
  investments: number;
  protectedForNeeds: number | null;
  availableForWants: number | null;
}

export interface Notice {
  id: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  date?: string;
  createdAt?: string;
}

export interface Feedback {
  id: string;
  userId?: string;
  userName?: string;
  userEmail?: string;
  subject: string;
  category?: string;
  message: string;
  status: string;
  createdAt?: string;
}

export interface FeedbackHistory {
  action?: string;
  note?: string;
  at?: string;
  by?: string;
}

export interface NewsItem {
  id: string;
  title: string;
  body: string;
  priority?: string;
  status?: string;
  authorName?: string;
  createdAt?: string;
}

export interface AdminInsights {
  totalMembers: number;
  activeMembers: number;
  inactiveMembers: number;
  adminCount: number;
  setupDone: number;
  setupPending: number;
  feedbackTotal: number;
  feedbackOpen: number;
  newsTotal: number;
  publishedNews: number;
  joinLabels: string[];
  joinValues: number[];
  statusLabels: string[];
  statusValues: number[];
  setupLabels: string[];
  setupValues: number[];
  recentMembers: VaultUser[];
}

export interface DashboardPayload {
  user?: VaultUser | null;
  finance?: FinanceProfile | null;
  stats?: DashStats | null;
  transactions?: Tx[];
  goals?: Goal[];
  savings?: Saving[];
  savingsOverview?: SavingsOverview | null;
  budgets?: Budget[];
  reports?: Report[];
  affordChecks?: AffordCheck[];
  notifications?: Notice[];
  feedback?: Feedback[];
  allFeedback?: Feedback[];
  news?: NewsItem[];
  allNews?: NewsItem[];
  members?: VaultUser[];
  admins?: VaultUser[];
  adminInsights?: AdminInsights | null;
  setupSkipped?: boolean;
}
