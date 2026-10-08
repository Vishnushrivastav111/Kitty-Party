import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  get<T>(path: string): Promise<T> {
    return this.send<T>('GET', path);
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.send<T>('POST', path, body);
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.send<T>('PUT', path, body);
  }

  delete(path: string): Promise<void> {
    return this.send<void>('DELETE', path);
  }

  raw<T>(method: string, path: string, body?: unknown): Promise<T> {
    return this.send<T>(method, path, body, false);
  }

  private async send<T>(method: string, path: string, body?: unknown, unwrap = true): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.http.request(method, this.base + path, {
          body,
          headers: { Accept: 'application/json' },
          responseType: 'text',
          observe: 'response',
        }),
      );
      const text = response.body?.trim() ?? '';
      if (!text) return undefined as T;
      const payload = JSON.parse(text) as unknown;
      if (!unwrap) return payload as T;
      if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
        return (payload as { data: T }).data;
      }
      return payload as T;
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        const body = error.error;
        const message =
          (body && typeof body === 'object' && 'message' in body && String(body.message)) ||
          error.message ||
          'Request failed';
        throw new ApiError(message, error.status);
      }
      throw error;
    }
  }
}
