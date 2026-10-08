import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { Validators, formatDate, formatINR, num, queryRows, todayISO } from '../../../shared/format';
import { AffordCheck } from '../../../model/models';
import { FinanceService } from '../../../service/finance.service';
import { PagerComponent } from '../../../shared/pager.component';

@Component({
  selector: 'app-affordability',
  standalone: true,
  imports: [FormsModule, PagerComponent],
  templateUrl: './affordability.component.html',
  styleUrl: './affordability.component.scss',
})
export class AffordabilityComponent {
  private readonly vault = inject(FinanceService);
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
