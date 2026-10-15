import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteFooterComponent } from '../../../shared/site-footer.component';
import { TermsContentComponent } from './terms-content.component';

/** Read-only Terms page reached from the footer. Acceptance happens in the registration dialog. */
@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [RouterLink, SiteFooterComponent, TermsContentComponent],
  templateUrl: './terms.component.html',
  styleUrl: './terms.component.scss',
})
export class TermsComponent {}
