import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { API_CAPABILITIES } from '../../../shared/finance-rules';
import { queryRows } from '../../../shared/format';
import { PagerComponent } from '../../../shared/pager.component';

export interface ApprovalRow {
  id: string;
  type: string;
  requester: string;
  requestedAt: string;
  details: string;
  status: string;
  reviewer?: string;
  reviewedAt?: string;
}

@Component({
  selector: 'app-admin-approvals',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './admin-approvals.component.html',
  styleUrl: './admin-approvals.component.scss',
})
export class AdminApprovalsComponent {
  readonly capabilities = API_CAPABILITIES;
  readonly search = signal('');
  readonly status = signal('pending');
  readonly type = signal('');
  readonly pageNo = signal(1);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly rows = signal<ApprovalRow[]>([]);
  readonly page = computed(() => queryRows(this.rows(), this.search(), ['type', 'requester', 'details', 'status'], { status: this.status() === 'all' ? '' : this.status(), type: this.type() }, this.pageNo(), 6));

  constructor() {
    this.loading.set(false);
    this.rows.set([]);
  }
}
