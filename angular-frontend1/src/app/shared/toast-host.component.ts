import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    <div class="mv-toasts" aria-live="polite" aria-atomic="false">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="mv-toast" [class.error]="toast.kind === 'error'" [attr.role]="toast.kind === 'error' ? 'alert' : 'status'">
          <i class="fas" [class.fa-circle-check]="toast.kind === 'success'" [class.fa-circle-exclamation]="toast.kind === 'error'"></i>
          <span>{{ toast.message }}</span>
          <button type="button" aria-label="Dismiss message" (click)="toasts.dismiss(toast.id)"><i class="fas fa-times"></i></button>
        </div>
      }
    </div>
  `,
  styles: [`
    .mv-toasts { position: fixed; top: 18px; right: 18px; z-index: 3000; display: flex; flex-direction: column; gap: 10px; max-width: min(380px, calc(100vw - 36px)); pointer-events: none; }
    .mv-toast { pointer-events: auto; display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: 12px; background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.18); font-weight: 600; font-size: 0.92rem; animation: mv-toast-in 0.22s ease-out; }
    .mv-toast.error { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
    .mv-toast > i { font-size: 1.1rem; }
    .mv-toast span { flex: 1; }
    .mv-toast button { background: none; border: 0; color: inherit; opacity: 0.6; cursor: pointer; padding: 2px 4px; }
    .mv-toast button:hover { opacity: 1; }
    @keyframes mv-toast-in { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
    @media (max-width: 600px) { .mv-toasts { top: auto; bottom: 14px; right: 14px; left: 14px; max-width: none; } }
    @media (prefers-reduced-motion: reduce) { .mv-toast { animation: none; } }
  `],
})
export class ToastHostComponent {
  readonly toasts = inject(ToastService);
}
