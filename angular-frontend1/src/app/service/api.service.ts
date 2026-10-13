import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public details?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  get<T>(path: string): Promise<T> { return this.send<T>('GET', path); }
  post<T>(path: string, body?: unknown): Promise<T> { return this.send<T>('POST', path, body); }
  put<T>(path: string, body?: unknown): Promise<T> { return this.send<T>('PUT', path, body); }
  delete(path: string): Promise<void> { return this.send<void>('DELETE', path); }
  raw<T>(method: string, path: string, body?: unknown): Promise<T> { return this.send<T>(method, path, body, false); }

  private async send<T>(method: string, path: string, body?: unknown, unwrap = true): Promise<T> {
    try {
      const response = await firstValueFrom(this.http.request(method, this.base + path, {
        body,
        headers: { Accept: 'application/json' },
        responseType: 'text',
        observe: 'response',
      }));
      return this.parsePayload<T>(response.body, unwrap);
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        const parsed = this.parseErrorBody(error.error);
        throw new ApiError(parsed.message || error.message || `Request failed with status ${error.status}`, error.status, parsed.details);
      }
      throw error;
    }
  }

  private parsePayload<T>(body: string | null, unwrap: boolean): T {
    const text = body?.trim() ?? '';
    if (!text) return undefined as T;
    try {
      const payload = JSON.parse(text) as unknown;
      if (!unwrap) return payload as T;
      if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
        return (payload as { data: T }).data;
      }
      return payload as T;
    } catch {
      return text as T;
    }
  }

  private parseErrorBody(body: unknown): { message: string; details?: unknown } {
    let value: unknown = body;
    if (typeof value === 'string') {
      try { value = JSON.parse(value); } catch { return { message: value }; }
    }
    if (value && typeof value === 'object') {
      const row = value as Record<string, unknown>;
      const candidates = [row['message'], row['error'], row['detail'], row['title']];
      const message = candidates.find((item) => typeof item === 'string' && item.trim()) as string | undefined;
      if (message) return { message: message.trim(), details: row['errors'] ?? row['fieldErrors'] ?? row };
      if (row['errors'] && typeof row['errors'] === 'object') {
        const messages = Object.entries(row['errors'] as Record<string, unknown>)
          .flatMap(([key, val]) => Array.isArray(val) ? val.map(String) : [String(val)])
          .filter(Boolean);
        if (messages.length) return { message: messages.join(' '), details: row['errors'] };
      }
    }
    return { message: typeof body === 'string' ? body : '' };
  }
}
