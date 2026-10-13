import { Validators, Pager, asArray, asStringList, formatDate, formatINR, homePath, monthStartISO, num, queryRows, todayISO } from './format';

describe('format.ts', () => {
  it('should format INR values', () => expect(formatINR(123456)).toBe('₹1,23,456'));
  it('should format valid dates and preserve invalid values', () => {
    expect(formatDate('2026-01-02')).toContain('02');
    expect(formatDate('not-a-date')).toBe('not-a-date');
    expect(formatDate()).toBe('—');
  });
  it('should return current ISO date helpers', () => {
    expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(monthStartISO()).toMatch(/^\d{4}-\d{2}-01$/);
  });
  it('should normalize arrays and comma/json string lists', () => {
    expect(asArray<number>([1, 2])).toEqual([1, 2]);
    expect(asArray<number>('x')).toEqual([]);
    expect(asStringList('a, b')).toEqual(['a', 'b']);
    expect(asStringList('["a","b"]')).toEqual(['a', 'b']);
    expect(asStringList(null)).toEqual([]);
  });
  it('should normalize numbers and resolve home paths', () => {
    expect(num('12')).toBe(12);
    expect(num('bad')).toBe(0);
    expect(homePath('admin')).toBe('/admin');
    expect(homePath('user')).toBe('/dashboard');
  });
  it('should query, filter and paginate rows', () => {
    const rows = [{ name: 'Alice', status: 'active' }, { name: 'Bob', status: 'inactive' }];
    expect(queryRows(rows, 'ali', ['name'], { status: '' }, 1, 1)).toEqual({ rows: [rows[0]], total: 1, totalPages: 1, page: 1 });
    expect(queryRows(rows, '', ['name'], { status: 'inactive' }, 1, 10).rows).toEqual([rows[1]]);
  });
  it('should validate common form values', () => {
    expect(Validators.required('')).toContain('required');
    expect(Validators.email('bad')).toContain('valid');
    expect(Validators.email('a@b.com')).toBe('');
    expect(Validators.phone('123')).toContain('valid');
    expect(Validators.phone('9876543210')).toBe('');
    expect(Validators.password('weak')).toContain('8');
    expect(Validators.password('Strong1!x')).toBe('');
    expect(Validators.name('A')).toContain('3');
    expect(Validators.name('Alice')).toBe('');
    expect(Validators.amount(-1)).toContain('non-negative');
    expect(Validators.amount(10)).toBe('');
  });
  it('should manage Pager state', () => {
    const pager = new Pager(1, () => [{ name: 'Alice' }, { name: 'Bob' }], ['name']);
    expect(pager.totalPages()).toBe(2);
    pager.setSearch('bob');
    expect(pager.rows()).toEqual([{ name: 'Bob' }]);
    pager.setSearch('');
    pager.next();
    expect(pager.page).toBe(2);
    pager.prev();
    expect(pager.page).toBe(1);
    pager.setFilter('name', 'Bob');
    expect(pager.rows()).toEqual([{ name: 'Bob' }]);
  });
});
