import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../service/api.service';
import { API_CAPABILITIES } from '../../../shared/finance-rules';
import { parseApiError } from '../../../shared/api-error';
import { queryRows } from '../../../shared/format';
import { PagerComponent } from '../../../shared/pager.component';

export interface ApprovalRow { id: string; requesterId: string; requesterName?: string; requesterEmail?: string; requestType: string; resourceId: string; requestedAt?: string; createdAt?: string; details?: string; reason?: string; status: string; reviewer?: string; reviewerName?: string; reviewedAt?: string; }

@Component({ selector: 'app-admin-approvals', standalone: true, imports: [FormsModule, PagerComponent], templateUrl: './admin-approvals.component.html', styleUrl: './admin-approvals.component.scss' })
export class AdminApprovalsComponent {
  private readonly api = inject(ApiService);
  readonly capabilities = API_CAPABILITIES; readonly search = signal(''); readonly status = signal('pending'); readonly type = signal(''); readonly pageNo = signal(1); readonly loading = signal(false); readonly error = signal(''); readonly rows = signal<ApprovalRow[]>([]);
  readonly page = computed(() => queryRows(this.rows(), this.search(), ['requestType', 'requesterName', 'requesterEmail', 'resourceId', 'reason', 'status'], { status: this.status() === 'all' ? '' : this.status(), type: this.type() }, this.pageNo(), 6));

  constructor() { void this.load(); }
  async load(): Promise<void> { this.loading.set(true); this.error.set(''); try { const rows = await this.api.get<ApprovalRow[]>('/admin/approvals'); this.rows.set(rows.map((row) => ({ ...row, requestedAt: row.createdAt, details: row.reason || row.resourceId }))); } catch (error) { this.error.set(parseApiError(error).message); } finally { this.loading.set(false); } }
  async approve(row: ApprovalRow): Promise<void> { this.loading.set(true); this.error.set(''); try { await this.api.post(`/admin/approvals/${row.id}/approve`, {}); await this.load(); } catch (error) { this.error.set(parseApiError(error).message); this.loading.set(false); } }
  async reject(row: ApprovalRow): Promise<void> { this.loading.set(true); this.error.set(''); try { await this.api.post(`/admin/approvals/${row.id}/reject`, {}); await this.load(); } catch (error) { this.error.set(parseApiError(error).message); this.loading.set(false); } }
}
