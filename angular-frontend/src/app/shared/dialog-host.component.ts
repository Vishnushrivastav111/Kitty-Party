import { Component, HostListener, inject } from '@angular/core';
import { DialogService } from '../core/dialog.service';

@Component({
  selector: 'app-dialog-host',
  standalone: true,
  template: `
    @if (dialog.state(); as state) {
      <div class="mv-confirm-backdrop open" (click)="backdrop($event)">
        <div class="mv-confirm-dialog" role="dialog" aria-modal="true">
          <div class="mv-confirm-icon" [class.info]="state.info">
            <i class="fas" [class.fa-circle-info]="state.info" [class.fa-trash]="!state.info"></i>
          </div>
          <h3>{{ state.title }}</h3>
          <p>{{ state.message }}</p>
          <div class="mv-confirm-actions">
            @if (!state.info) {
              <button type="button" class="btn btn-outline" (click)="dialog.close(false)">{{ state.cancelText }}</button>
            }
            <button type="button" class="btn" [class.btn-primary]="state.info" [class.btn-danger]="!state.info" (click)="dialog.close(true)">
              {{ state.okText }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class DialogHostComponent {
  readonly dialog = inject(DialogService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.dialog.state()) this.dialog.close(false);
  }

  backdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.dialog.close(!!this.dialog.state()?.info);
    }
  }
}
