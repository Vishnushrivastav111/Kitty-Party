import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  kind: 'success' | 'error';
}

const LIFETIME_MS = 3800;
/** Messages that arrive this close together replace each other instead of stacking. */
const MERGE_WINDOW_MS = 1500;

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private next = 1;
  private lastShownAt = 0;

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  dismiss(id: number): void {
    this.toasts.update((rows) => rows.filter((row) => row.id !== id));
  }

  private show(message: string, kind: Toast['kind']): void {
    if (!message) return;
    const now = Date.now();
    const merge = now - this.lastShownAt < MERGE_WINDOW_MS;
    this.lastShownAt = now;
    const toast: Toast = { id: this.next++, message, kind };
    this.toasts.update((rows) => [...(merge ? rows.filter((row) => row.kind !== kind) : rows), toast].slice(-3));
    setTimeout(() => this.dismiss(toast.id), LIFETIME_MS);
  }
}
