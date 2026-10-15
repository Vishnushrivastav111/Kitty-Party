import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, signal } from '@angular/core';
import { TermsContentComponent } from './terms-content.component';

/** Modal shown on top of the registration page. Accept unlocks only after the text is scrolled to the end. */
@Component({
  selector: 'app-terms-dialog',
  standalone: true,
  imports: [TermsContentComponent],
  template: `
    <div class="modal-backdrop" [class.open]="open" (click)="close.emit()">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="termsTitle" style="width:min(640px,100%)" (click)="$event.stopPropagation()">
        <h3 id="termsTitle">Terms &amp; Conditions</h3>
        <div #box class="terms-box" tabindex="0" (scroll)="check()">
          <app-terms-content />
        </div>
        @if (!read()) {
          <p class="terms-hint" role="status">Scroll to the end of the terms to enable the accept button.</p>
        }
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" (click)="close.emit()">Close</button>
          <button type="button" class="btn btn-primary" [disabled]="!read()" (click)="accept.emit()">I have read and accept</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .terms-box { max-height: 55vh; overflow: auto; border: 1px solid var(--line); border-radius: 8px; padding: 4px 16px 12px; }
    .terms-hint { margin: 10px 0 0; font-size: 13px; color: var(--slate); }
  `],
})
export class TermsDialogComponent implements OnChanges {
  @Input() open = false;
  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly accept = new EventEmitter<void>();
  @ViewChild('box') private box?: ElementRef<HTMLElement>;

  readonly read = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    // If the text is short enough that nothing scrolls, there is nothing left to read.
    if (changes['open'] && this.open) setTimeout(() => this.check());
  }

  check(): void {
    const el = this.box?.nativeElement;
    if (el && el.scrollTop + el.clientHeight >= el.scrollHeight - 8) this.read.set(true);
  }
}
