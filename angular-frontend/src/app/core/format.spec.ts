import {
  Pager,
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
  ROLES,
} from './format';

describe('format utilities', () => {
  it('returns today and month start in ISO format', () => {
    expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(monthStartISO()).toMatch(/^\d{4}-\d{2}-01$/);
  });

  it('formats INR and dates', () => {
    expect(formatINR(123456)).toBe('₹1,23,456');
    expect(formatINR('bad')).toBe('₹0');
    expect(formatDate()).toBe('—');
    expect(formatDate(null)).toBe('—');
    expect(formatDate('2026-01-15')).toContain('15');
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });

  it('normalizes arrays and string lists', () => {
    expect(asArray<number>([1, 2])).toEqual([1, 2]);
    expect(asArray<number>('x')).toEqual([]);
    expect(asStringList(['a', 2])).toEqual(['a', '2']);
    expect(asStringList('["a",2]')).toEqual(['a', '2']);
    expect(asStringList('a,b, c')).toEqual(['a', 'b', 'c']);
    expect(asStringList('')).toEqual([]);
    expect(asStringList(null)).toEqual([]);
  });

  it('queries, filters and paginates rows', () => {
    const rows = [
      { name: 'Alice', role: 'user' },
      { name: 'Bob', role: 'admin' },
      { name: 'Carol', role: 'user' },
    ];
    expect(queryRows(rows, 'ali', ['name'], {}, 1, 10).rows).toEqual([rows[0]]);
    expect(queryRows(rows, '', ['name'], { role: 'user' }, 1, 10).total).toBe(2);
    expect(queryRows(rows, '', ['name'], {}, 99, 2).page).toBe(2);
    expect(queryRows(rows, '', ['name'], {}, 1, 2).totalPages).toBe(2);
  });

  it('handles numeric and role routing helpers', () => {
    expect(num('12')).toBe(12);
    expect(num('bad')).toBe(0);
    expect(num(Infinity)).toBe(0);
    expect(homePath(ROLES.ADMIN)).toBe('/admin');
    expect(homePath(ROLES.SUPERADMIN)).toBe('/admin');
    expect(homePath(ROLES.USER)).toBe('/dashboard');
  });

  it('validates required, email, phone, password, name, amount and date', () => {
    expect(Validators.required('')).toBe('This field is required');
    expect(Validators.required('ok')).toBe('');
    expect(Validators.email('')).toBe('Email is required');
    expect(Validators.email('bad')).toContain('valid');
    expect(Validators.email('a@test.com')).toBe('');
    expect(Validators.phone('')).toBe('Phone is required');
    expect(Validators.phone('123')).toContain('valid');
    expect(Validators.phone('9876543210')).toBe('');
    expect(Validators.password('')).toBe('Password is required');
    expect(Validators.password('short')).toContain('8');
    expect(Validators.password('abcdefgh')).toContain('uppercase');
    expect(Validators.password('Abcdefgh')).toContain('number');
    expect(Validators.password('Abcdefg1')).toContain('special');
    expect(Validators.password('Abcdefg1!')).toBe('');
    expect(Validators.name('ab')).toContain('3');
    expect(Validators.name('John123')).toContain('letters');
    expect(Validators.name('John Doe')).toBe('');
    expect(Validators.amount('')).toContain('required');
    expect(Validators.amount(-1)).toContain('non-negative');
    expect(Validators.amount(10)).toBe('');
    expect(Validators.dateNotFuture('')).toContain('required');
    expect(Validators.dateNotFuture('2999-01-01')).toContain('future');
    expect(Validators.dateNotFuture(todayISO())).toBe('');
  });

  it('supports Pager search, filters and navigation', () => {
    const source = () => [
      { name: 'Alice', role: 'user' },
      { name: 'Bob', role: 'admin' },
      { name: 'Carol', role: 'user' },
    ];
    const pager = new Pager(1, source, ['name', 'role']);
    expect(pager.totalPages()).toBe(3);
    expect(pager.rows()[0].name).toBe('Alice');
    pager.next();
    expect(pager.rows()[0].name).toBe('Bob');
    pager.prev();
    expect(pager.rows()[0].name).toBe('Alice');
    pager.setSearch('car');
    expect(pager.rows()[0].name).toBe('Carol');
    pager.setFilter('role', 'admin');
    expect(pager.rows()).toEqual([]);
    pager.setFilter('role', '');
    expect(pager.rows()[0].name).toBe('Carol');
  });
});
