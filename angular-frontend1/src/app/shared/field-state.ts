export function assignError(current: Record<string, string>, field: string, message: string): Record<string, string> {
  const next = { ...current };
  if (message) next[field] = message;
  else delete next[field];
  return next;
}

export function confirmError(password: string, confirm: string): string {
  if (!confirm) return password ? '' : '';
  return confirm !== password ? 'Passwords do not match' : '';
}

export function assignErrors(current: Record<string, string>, updates: Record<string, string>): Record<string, string> {
  let next = { ...current };
  for (const [field, message] of Object.entries(updates)) {
    next = assignError(next, field, message);
  }
  return next;
}

/** Places backend field errors on the inputs this form actually renders. */
export function mergeApiFields(current: Record<string, string>, incoming: Record<string, string> = {}): Record<string, string> {
  const next = assignErrors(current, incoming);
  const fullName = incoming['fullName'];
  if (fullName && !incoming['firstName']) next['firstName'] = next['firstName'] || fullName;
  if (fullName && !incoming['lastName']) next['lastName'] = next['lastName'] || fullName;
  return next;
}
