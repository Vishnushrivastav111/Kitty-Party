import { Component, inject, OnInit, signal } from '@angular/core';
import { AdminService, ApprovalRequest } from '../../service/admin.service';
import { DialogService } from '../../shared/dialog.service';
import { formatDate, formatINR } from '../../shared/format';

@Component({
  selector: 'app-admin-approvals',
  standalone: true,
  templateUrl: './admin-approvals.component.html',
  styleUrl: './admin-approvals.component.scss',
})
export class AdminApprovalsComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly dialog = inject(DialogService);
  readonly rows = signal<ApprovalRequest[]>([]);
  readonly loading = signal(false);
  readonly busy = signal<string | null>(null);
  readonly date = formatDate;
  readonly money = formatINR;

  async ngOnInit(): Promise<void> { await this.load(); }

  async load(): Promise<void> {
    this.loading.set(true);
    try { this.rows.set(await this.admin.getApprovals()); }
    catch (error) { await this.dialog.notice(error instanceof Error ? error.message : 'Could not load approvals'); }
    finally { this.loading.set(false); }
  }

  async approve(row: ApprovalRequest): Promise<void> {
    if (!await this.dialog.confirm('Approve this request?')) return;
    this.busy.set(row.id);
    try { await this.admin.approveRequest(row.id); await this.load(); }
    catch (error) { await this.dialog.notice(error instanceof Error ? error.message : 'Could not approve request'); }
    finally { this.busy.set(null); }
  }

  async reject(row: ApprovalRequest): Promise<void> {
    if (!await this.dialog.confirm('Reject this request?')) return;
    this.busy.set(row.id);
    try { await this.admin.rejectRequest(row.id); await this.load(); }
    catch (error) { await this.dialog.notice(error instanceof Error ? error.message : 'Could not reject request'); }
    finally { this.busy.set(null); }
  }
}
