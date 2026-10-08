import { Component, HostListener, inject } from '@angular/core';
import { DialogService } from './dialog.service';

@Component({
  selector: 'app-dialog-host',
  standalone: true,
  templateUrl: './dialog-host.component.html',
  styleUrl: './dialog-host.component.scss',
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
