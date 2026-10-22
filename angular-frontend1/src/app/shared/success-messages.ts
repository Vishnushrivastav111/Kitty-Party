/**
 * Plain-language success messages for completed changes, so every screen confirms what just happened.
 * Pure on purpose: it is used by the HTTP interceptor and is covered by the node rules tests.
 */

const NOUNS: Record<string, { one: string; many: string }> = {
  transactions: { one: 'Transaction', many: 'transactions' },
  goals: { one: 'Goal', many: 'goals' },
  savings: { one: 'Savings entry', many: 'savings entries' },
  budgets: { one: 'Budget', many: 'budgets' },
  reports: { one: 'Report', many: 'reports' },
  affordability: { one: 'Affordability check', many: 'affordability checks' },
  notifications: { one: 'Notification', many: 'notifications' },
  feedback: { one: 'Feedback', many: 'feedback entries' },
  news: { one: 'News item', many: 'news items' },
};

const VERBS: Record<string, string> = { POST: 'added', PUT: 'updated', DELETE: 'deleted' };

const ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** The path after `/api`, without query string. */
export function apiPath(url: string): string | null {
  const match = /\/api(\/[^?#]*)/.exec(url);
  return match ? match[1].replace(/\/+$/, '') || '/' : null;
}

/** Returns the message to show after a successful request, or null when nothing should be shown. */
export function successMessage(method: string, url: string): string | null {
  const verb = VERBS[method.toUpperCase()];
  const path = apiPath(url);
  if (!verb || !path) return null;
  const parts = path.split('/').filter(Boolean);
  const key = parts.join('/');

  const fixed: Record<string, string> = {
    'POST auth/login': 'Signed in successfully',
    'POST auth/logout': 'Signed out successfully',
    'POST auth/register': 'Account created successfully',
    'POST auth/forgot/send-otp': 'Verification code sent successfully',
    'POST auth/forgot/reset': 'Password reset successfully',
    'PUT finance': 'Financial profile saved successfully',
    'POST finance/skip': '',
    'POST affordability/check': 'Affordability check completed successfully',
    'POST reports/generate': 'Report generated successfully',
    'PUT users/me': 'Profile updated successfully',
    'PUT users/me/password': 'Password changed successfully',
    'PUT notifications/read-all': 'All notifications marked as read',
    'POST admin/approvals': 'Request submitted successfully',
  };
  const direct = fixed[`${method.toUpperCase()} ${key}`];
  if (direct !== undefined) return direct || null;

  if (parts[0] === 'notifications' && parts[2] === 'read') return 'Notification marked as read';
  if (parts[0] === 'admin' && parts[1] === 'approvals' && parts[3] === 'approve') return 'Request approved successfully';
  if (parts[0] === 'admin' && parts[1] === 'approvals' && parts[3] === 'reject') return 'Request rejected successfully';
  if (parts[0] === 'admin' && parts[1] === 'users') return `User ${verb} successfully`;
  if (parts[0] === 'admin' && parts[1] === 'admins') return `Admin ${verb} successfully`;
  if (parts[0] === 'admin' && parts[1] === 'news') return `News item ${verb} successfully`;

  const noun = NOUNS[parts[0]];
  if (!noun) return null;
  if (parts.length >= 3 && !ID.test(parts[1])) return null;
  const single = parts.length >= 2 && ID.test(parts[1]);
  if (single && parts.length === 2) return `${noun.one} ${verb} successfully`;
  if (parts.length === 1 && method.toUpperCase() === 'DELETE') return `All ${noun.many} deleted successfully`;
  if (parts.length === 1) return `${noun.one} ${verb} successfully`;
  if (single && parts[2] === 'history') return null;
  return null;
}
