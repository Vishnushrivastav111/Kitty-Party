import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/dialog.service';
import { Validators, formatDate, formatINR, num, queryRows, todayISO } from '../core/format';
import { AffordCheck } from '../core/models';
import { VaultService } from '../core/vault.service';
import { PagerComponent } from '../shared/pager.component';

@Component({
  selector: 'app-affordability',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  template: `
    <div class="page-header"><h1>Affordability</h1><p>Check whether a purchase fits your income, expenses, and savings</p></div>
    <div class="panel">
      <h2 style="margin-bottom:14px;color:var(--navy)">Can I afford this?</h2>
      <form (ngSubmit)="check()" novalidate>
        <div class="form-grid">
          <div class="form-group"><label>Item / purchase name</label><input name="itemName" [(ngModel)]="itemName" placeholder="e.g. Laptop, scooter, vacation" /><span class="field-error" [style.display]="errors()['itemName'] ? 'block' : 'none'">{{ errors()['itemName'] }}</span></div>
          <div class="form-group"><label>Estimated cost (₹)</label><input type="number" name="amount" min="1" [(ngModel)]="amount" /><span class="field-error" [style.display]="errors()['amount'] ? 'block' : 'none'">{{ errors()['amount'] }}</span></div>
          <div class="form-group"><label>Check date</label><input type="date" name="checkDate" [(ngModel)]="checkDate" [max]="today" /><span class="field-error" [style.display]="errors()['checkDate'] ? 'block' : 'none'">{{ errors()['checkDate'] }}</span></div>
          <div class="form-group"><label>Priority</label><select name="priority" [(ngModel)]="priority"><option>Need</option><option>Want</option><option>Investment</option></select></div>
        </div>
        <div class="form-actions"><button class="btn btn-primary" [disabled]="busy()"><i class="fas fa-calculator"></i> Check affordability</button></div>
      </form>
      @if (result(); as row) {
        <div class="alert-box" [class.success]="row.level === 'success'" [class.warning]="row.level === 'warning'" [class.danger]="row.level === 'danger'" style="margin-top:16px">
          <strong>{{ row.verdict }}</strong><br />
          {{ row.itemName }} · {{ money(row.amount) }} · available {{ money(row.available || row.availableAmount) }}
          @if (row.suggestion) { <div>{{ row.suggestion }}</div> }
        </div>
      }
    </div>
    <div class="panel">
      <div class="panel-header">
        <h2>Previous checks</h2>
        <button class="btn btn-danger btn-sm" type="button" (click)="clearAll()"><i class="fas fa-trash"></i> Clear all</button>
      </div>
      <div class="toolbar">
        <input type="search" placeholder="Search item..." [ngModel]="search()" (ngModelChange)="search.set($event); pageNo.set(1)" />
        <select [ngModel]="level()" (ngModelChange)="level.set($event); pageNo.set(1)">
          <option value="">All results</option><option value="success">Comfortable</option><option value="warning">Caution</option><option value="danger">Not recommended</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Date</th><th>Item</th><th>Cost</th><th>Available</th><th>Verdict</th><th>Priority</th><th>Actions</th></tr></thead>
          <tbody>
            @if (!page().rows.length) { <tr><td colspan="7" class="empty-state">No checks yet</td></tr> }
            @for (row of page().rows; track row.id) {
              <tr>
                <td>{{ date(row.date) }}</td><td>{{ row.itemName }}</td><td>{{ money(row.amount) }}</td><td>{{ money(row.available || row.availableAmount) }}</td>
                <td><span class="badge" [class.badge-success]="row.level === 'success'" [class.badge-warning]="row.level === 'warning'" [class.badge-danger]="row.level === 'danger'">{{ row.verdict }}</span></td>
                <td>{{ row.priority }}</td>
                <td><button class="btn btn-danger btn-sm" type="button" (click)="remove(row)"><i class="fas fa-trash"></i></button></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page().page" [totalPages]="page().totalPages" [total]="page().total" (pageChange)="pageNo.set($event)" />
    </div>
  `,
})
export class AffordabilityComponent {
  private readonly vault = inject(VaultService);
  private readonly dialog = inject(DialogService);
  readonly today = todayISO();
  readonly money = formatINR;
  readonly date = formatDate;
  itemName = '';
  amount: number | null = null;
  checkDate = todayISO();
  priority = 'Need';
  readonly result = signal<AffordCheck | null>(null);
  readonly search = signal('');
  readonly level = signal('');
  readonly pageNo = signal(1);
  readonly busy = signal(false);
  readonly errors = signal<Record<string, string>>({});
  readonly page = computed(() => queryRows(this.vault.affordChecks(), this.search(), ['itemName', 'verdict', 'priority'], { level: this.level() }, this.pageNo(), 5));

  async check(): Promise<void> {
    const errors = {
      itemName: Validators.required(this.itemName, 'Item name'),
      amount: Number(this.amount) > 0 ? '' : 'Amount must be greater than zero',
      checkDate: Validators.dateNotFuture(this.checkDate, 'Check date'),
    };
    this.errors.set(errors);
    if (Object.values(errors).some(Boolean)) return;
    this.busy.set(true);
    try {
      const row = await this.vault.checkAfford({ itemName: this.itemName.trim(), amount: Number(this.amount), checkDate: this.checkDate, priority: this.priority });
      this.result.set(row);
    } catch (error) {
      await this.dialog.notice(error instanceof Error ? error.message : 'Could not check affordability');
    } finally { this.busy.set(false); }
  }
  async remove(row: AffordCheck): Promise<void> {
    if (await this.dialog.confirm('Are you sure you want to delete this check?')) await this.vault.deleteAfford(row.id);
  }
  async clearAll(): Promise<void> {
    if (await this.dialog.confirm('Delete every affordability check?')) await this.vault.clearAfford();
  }
}
