import { Injectable, signal } from '@angular/core';

export interface DialogState {
  title: string;
  message: string;
  okText: string;
  cancelText: string;
  info: boolean;
}

@Injectable({ providedIn: 'root' })
export class DialogService {
  readonly state = signal<DialogState | null>(null);
  private resolve: ((value: boolean) => void) | null = null;

  confirm(message: string, options: { title?: string; okText?: string; cancelText?: string; mode?: 'delete' | 'info' } = {}): Promise<boolean> {
    const info = options.mode === 'info';
    this.state.set({
      title: options.title || (info ? 'Notice' : 'Confirm delete'),
      message,
      okText: options.okText || (info ? 'OK' : 'Delete'),
      cancelText: options.cancelText || 'Cancel',
      info,
    });
    return new Promise((resolve) => {
      this.resolve = resolve;
    });
  }

  notice(message: string, title = 'Notice'): Promise<boolean> {
    return this.confirm(message, { mode: 'info', title, okText: 'OK' });
  }

  close(value: boolean): void {
    this.state.set(null);
    this.resolve?.(value);
    this.resolve = null;
  }
}
