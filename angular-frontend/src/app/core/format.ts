export const ROLES = { USER: 'user', ADMIN: 'admin', SUPERADMIN: 'superadmin' } as const;

export const CHART_COLORS = [
  '#0d9488', '#0284c7', '#d97706', '#dc2626', '#7c3aed',
  '#059669', '#ea580c', '#0b3a5c', '#db2777', '#4f46e5',
];

export const TX_CATEGORIES = [
  'Salary', 'Food', 'Rent', 'Transport', 'Shopping', 'Utilities', 'Healthcare', 'Entertainment', 'Other',
];

export const GOAL_CATEGORIES = ['Personal', 'Emergency', 'Travel', 'Education', 'Home', 'Other'];

export const SAVINGS_CATEGORIES = ['Opening', 'Deposit', 'Withdrawal', 'Interest', 'Transfer', 'Other'];

export const BUDGET_CATEGORIES = ['Food', 'Rent', 'Transport', 'Shopping', 'Utilities', 'Healthcare', 'Entertainment', 'Other'];

export function todayISO(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

export function monthStartISO(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  return `${d.getFullYear()}-${m}-01`;
}

export function formatINR(n: unknown): string {
  const num = Number(n) || 0;
  return '₹' + num.toLocaleString('en-IN', { maximumFractionDigits: 0 });
}

export function formatDate(iso?: string | null): string {
  if (!iso) return '—';
  const raw = String(iso).slice(0, 10);
  const d = new Date(raw + 'T00:00:00');
  if (Number.isNaN(d.getTime())) return String(iso);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function asArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

export function asStringList(value: unknown): string[] {
  if (Array.isArray(value)) return value.map((item) => String(item));
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value) as unknown;
      if (Array.isArray(parsed)) return parsed.map((item) => String(item));
    } catch {
      /* plain comma list */
    }
    return value.split(',').map((item) => item.trim()).filter(Boolean);
  }
  return [];
}

export function queryRows<T extends object>(
  rows: T[],
  search: string,
  keys: (keyof T)[],
  filters: Record<string, string>,
  page: number,
  perPage: number,
): { rows: T[]; total: number; totalPages: number; page: number } {
  let list = rows.slice();
  const q = search.trim().toLowerCase();
  if (q) {
    list = list.filter((row) => keys.some((key) => String(row[key] ?? '').toLowerCase().includes(q)));
  }
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '') {
      list = list.filter((row) => String((row as Record<string, unknown>)[key] ?? '') === value);
    }
  });
  const total = list.length;
  const totalPages = Math.max(1, Math.ceil(total / perPage));
  const safe = Math.min(Math.max(1, page), totalPages);
  const start = (safe - 1) * perPage;
  return { rows: list.slice(start, start + perPage), total, totalPages, page: safe };
}

export function num(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export function homePath(role?: string): string {
  if (role === ROLES.ADMIN || role === ROLES.SUPERADMIN) return '/admin';
  return '/dashboard';
}

export const Validators = {
  required(v: unknown, label = 'This field'): string {
    if (v === undefined || v === null || String(v).trim() === '') return label + ' is required';
    return '';
  },
  email(v: string): string {
    if (!v) return 'Email is required';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) return 'Enter a valid email address';
    return '';
  },
  phone(v: string): string {
    if (!v) return 'Phone is required';
    if (!/^[6-9]\d{9}$/.test(v)) return 'Enter a valid 10-digit Indian mobile number';
    return '';
  },
  password(v: string): string {
    if (!v) return 'Password is required';
    if (v.length < 8) return 'Password must be at least 8 characters';
    if (!/[A-Z]/.test(v)) return 'Include at least one uppercase letter';
    if (!/[a-z]/.test(v)) return 'Include at least one lowercase letter';
    if (!/[0-9]/.test(v)) return 'Include at least one number';
    if (!/[^A-Za-z0-9]/.test(v)) return 'Include at least one special character';
    return '';
  },
  name(v: string): string {
    if (!v || v.trim().length < 3) return 'Full name must be at least 3 characters';
    if (!/^[A-Za-z\s.]+$/.test(v.trim())) return 'Name can only contain letters and spaces';
    return '';
  },
  amount(v: unknown, label = 'Amount'): string {
    if (v === '' || v === null || v === undefined) return label + ' is required';
    const n = Number(v);
    if (Number.isNaN(n) || n < 0) return label + ' must be a valid non-negative number';
    return '';
  },
  dateNotFuture(v: string, label = 'Date'): string {
    if (!v) return label + ' is required';
    if (v > todayISO()) return label + ' cannot be in the future';
    return '';
  },
};

export class Pager<T extends Record<string, unknown>> {
  page = 1;
  search = '';
  filters: Record<string, string> = {};

  constructor(
    public perPage: number,
    private source: () => T[],
    public keys: string[],
  ) {}

  filtered(): T[] {
    let rows = this.source();
    const q = this.search.trim().toLowerCase();
    if (q) {
      rows = rows.filter((r) => this.keys.some((k) => String(r[k] ?? '').toLowerCase().includes(q)));
    }
    Object.keys(this.filters).forEach((key) => {
      const val = this.filters[key];
      if (val !== '' && val !== null && val !== undefined) {
        rows = rows.filter((r) => String(r[key]) === String(val));
      }
    });
    return rows;
  }

  rows(): T[] {
    const all = this.filtered();
    const pages = Math.max(1, Math.ceil(all.length / this.perPage));
    const page = Math.min(this.page, pages);
    const start = (page - 1) * this.perPage;
    return all.slice(start, start + this.perPage);
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.filtered().length / this.perPage));
  }

  setSearch(value: string): void {
    this.search = value;
    this.page = 1;
  }

  setFilter(key: string, value: string): void {
    this.filters[key] = value;
    this.page = 1;
  }

  prev(): void {
    if (this.page > 1) this.page--;
  }

  next(): void {
    if (this.page < this.totalPages()) this.page++;
  }
}
