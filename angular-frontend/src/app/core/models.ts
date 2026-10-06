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
  payCycle?: string;
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
