export class ApiError extends Error {
  readonly fieldErrors: Record<string, string>;

  constructor(message: string, readonly status: number, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.fieldErrors = fieldErrors;
  }
}

const FIELD_HINTS: Array<[RegExp, string]> = [
  [/first name/i, 'firstName'],
  [/middle name/i, 'middleName'],
  [/last name/i, 'lastName'],
  [/full name/i, 'fullName'],
  [/e-?mail/i, 'email'],
  [/phone|mobile/i, 'phone'],
  [/confirm password|passwords do not match/i, 'confirmPassword'],
  [/current password/i, 'current'],
  [/password/i, 'password'],
  [/from date|start date/i, 'from'],
  [/to date|end date/i, 'to'],
  [/deadline/i, 'deadline'],
  [/title|subject/i, 'title'],
  [/target/i, 'target'],
  [/amount|limit/i, 'amount'],
  [/category/i, 'category'],
  [/otp|verification code/i, 'otp'],
];

export function parseApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (isHttpFailure(error)) {
    const status = Number(error.status) || 0;
    if (status === 0) {
      return new ApiError('We could not connect right now. Check your connection and try again.', 0);
    }
    const parsed = readErrorBody(error.error);
    const message = parsed.message || emptyStatusMessage(status);
    return new ApiError(message, status, parsed.fieldErrors);
  }
  if (error instanceof Error) {
    const message = sanitizeMessage(error.message);
    if (!message || /https?:\/\//i.test(error.message)) {
      return new ApiError('Something went wrong. Please try again.', 0);
    }
    return new ApiError(message, 0);
  }
  return new ApiError('Something went wrong. Please try again.', 0);
}

export function emptyStatusMessage(status: number): string {
  return `Something went wrong (code ${status}). Please try again.`;
}

function isHttpFailure(error: unknown): error is { status: number; error: unknown; message?: string } {
  return !!error && typeof error === 'object' && 'status' in error && 'error' in error && !(error instanceof ApiError);
}

function readErrorBody(body: unknown): { message: string; fieldErrors: Record<string, string> } {
  if (body == null || body === '') return { message: '', fieldErrors: {} };
  if (typeof body === 'string') {
    const trimmed = body.trim();
    if (!trimmed) return { message: '', fieldErrors: {} };
    if (/<\s*!doctype|<\s*html|<\s*body/i.test(trimmed)) return { message: '', fieldErrors: {} };
    try {
      return readErrorBody(JSON.parse(trimmed) as unknown);
    } catch {
      return { message: sanitizeMessage(trimmed), fieldErrors: {} };
    }
  }
  if (typeof body !== 'object') return { message: '', fieldErrors: {} };
  const record = body as Record<string, unknown>;
  const fieldErrors = {
    ...readStructuredFields(record),
    ...mapMessageToFields(typeof record['message'] === 'string' ? record['message'] : ''),
  };
  const message = typeof record['message'] === 'string' ? sanitizeMessage(record['message']) : '';
  return { message, fieldErrors };
}

function readStructuredFields(record: Record<string, unknown>): Record<string, string> {
  const fields: Record<string, string> = {};
  const buckets = [record['errors'], record['fieldErrors'], record['violations']];
  for (const bucket of buckets) {
    if (!bucket) continue;
    if (Array.isArray(bucket)) {
      for (const item of bucket) {
        if (!item || typeof item !== 'object') continue;
        const row = item as Record<string, unknown>;
        const key = String(row['field'] || row['property'] || row['name'] || '');
        const text = String(row['message'] || row['defaultMessage'] || '');
        if (!key || sensitiveKey(key) || !text) continue;
        fields[key] = sanitizeMessage(text);
      }
    } else if (typeof bucket === 'object') {
      for (const [key, value] of Object.entries(bucket as Record<string, unknown>)) {
        if (sensitiveKey(key) || value == null) continue;
        const text = Array.isArray(value) ? value.map(String).join(' ') : String(value);
        if (text) fields[key] = sanitizeMessage(text);
      }
    }
  }
  return fields;
}

function mapMessageToFields(message: string): Record<string, string> {
  const fields: Record<string, string> = {};
  for (const part of message.split(';').map((item) => item.trim()).filter(Boolean)) {
    const field = fieldFor(part);
    if (!field) continue;
    fields[field] = fields[field] ? `${fields[field]}; ${sanitizeMessage(part)}` : sanitizeMessage(part);
  }
  return fields;
}

function fieldFor(text: string): string {
  for (const [pattern, field] of FIELD_HINTS) {
    if (pattern.test(text)) return field;
  }
  return '';
}

function sensitiveKey(key: string): boolean {
  return /password|token|authorization|secret|credential/i.test(key);
}

export function sanitizeMessage(value: string): string {
  let text = value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
  text = text.replace(/bearer\s+\S+/gi, 'bearer [redacted]');
  text = text.replace(/\beyJ[a-zA-Z0-9_-]{8,}\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\b/g, '[redacted]');
  text = text.replace(/(password|token|secret|authorization)\s*[:=]\s*\S+/gi, '$1=[redacted]');
  text = text.replace(/https?:\/\/\S+/gi, '').trim();
  return text;
}
