package com.microvault.finance.service;

import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single definition of the user's savings picture. Every screen and the affordability check read it from here.
 *
 * <p>Savings are split into three buckets that never overlap, so nothing is counted twice:
 * <ul>
 *   <li><b>Emergency reserve</b>: the reserve recorded in financial setup (never more than the liquid savings
 *       it sits inside) plus later savings entries in the Emergency category.</li>
 *   <li><b>Unallocated liquid savings</b>: setup liquid savings minus that reserve, plus later general
 *       entries (no goal, not Emergency), less withdrawals.</li>
 *   <li><b>Allocated savings</b>: money set aside for goals (each goal's opening amount plus its linked
 *       contributions).</li>
 * </ul>
 * Total savings is the sum of the three. Investments are reported separately and are not savings.
 *
 * <p>Affordability: a need can use the unallocated liquid savings. A want can only use what is left
 * after keeping {@value #PROTECTED_MONTHS} months of income protected for needs and utilities.
 * The emergency reserve and goal savings are never available for either.
 */
public final class SavingsPosition {

    /** Months of income kept back from wants. */
    public static final int PROTECTED_MONTHS = 3;

    private SavingsPosition() {
    }

    /** All amounts are scale 2 and never negative. */
    public record Snapshot(BigDecimal allocated, BigDecimal unallocatedLiquid, BigDecimal emergencyReserve,
                           BigDecimal totalSavings, BigDecimal investments) {
    }

    public static Snapshot of(FinanceProfile profile, List<SavingsEntry> entries, List<Goal> goals) {
        BigDecimal liquid = profile == null ? FinancialPlan.money(null) : FinancialPlan.money(profile.getCurrentSavings());
        BigDecimal reserve = profile == null ? FinancialPlan.money(null) : FinancialPlan.money(profile.getEmergencyReserve());
        if (reserve.compareTo(liquid) > 0) {
            reserve = liquid;
        }
        BigDecimal unallocated = liquid.subtract(reserve);
        if (entries != null) {
            for (SavingsEntry entry : entries) {
                if (entry == null || entry.getAmount() == null || entry.getGoalId() != null) {
                    continue;
                }
                String category = entry.getCategory();
                if (FinancialPlan.emergencyWithdrawalCategory(category)) {
                    reserve = reserve.subtract(entry.getAmount().abs());
                } else if (FinancialPlan.emergencyCategory(category)) {
                    reserve = reserve.add(entry.getAmount());
                } else if (liquid.signum() > 0 && FinancialPlan.openingMirror(category)) {
                    continue;
                } else if (FinancialPlan.withdrawalCategory(category)) {
                    unallocated = unallocated.subtract(entry.getAmount().abs());
                } else {
                    unallocated = unallocated.add(entry.getAmount());
                }
            }
        }
        BigDecimal allocated = BigDecimal.ZERO;
        if (goals != null) {
            for (Goal goal : goals) {
                if (goal != null && goal.getSaved() != null) {
                    allocated = allocated.add(goal.getSaved());
                }
            }
        }
        BigDecimal emergency = FinancialPlan.nonNegative(reserve);
        BigDecimal free = FinancialPlan.nonNegative(unallocated);
        BigDecimal set = FinancialPlan.nonNegative(allocated);
        BigDecimal investments = profile == null ? FinancialPlan.money(null) : FinancialPlan.money(profile.getInvestments());
        return new Snapshot(set, free, emergency, FinancialPlan.money(set.add(free).add(emergency)), investments);
    }

    /** Three months of total monthly income. Zero when income is missing. */
    public static BigDecimal protectedForNeeds(BigDecimal totalMonthlyIncome) {
        BigDecimal income = FinancialPlan.nonNegative(totalMonthlyIncome);
        return FinancialPlan.money(income.multiply(BigDecimal.valueOf(PROTECTED_MONTHS)));
    }

    /**
     * Savings that can pay for a purchase.
     * Need: all unallocated liquid savings. Want (and anything that is not a need): only the amount above the
     * protected months of income. Never negative.
     */
    public static BigDecimal availableFor(Snapshot snapshot, BigDecimal totalMonthlyIncome, boolean need) {
        BigDecimal free = snapshot.unallocatedLiquid();
        if (need) {
            return FinancialPlan.money(free);
        }
        return FinancialPlan.nonNegative(free.subtract(protectedForNeeds(totalMonthlyIncome)));
    }

    public static boolean isNeed(String priority) {
        return priority != null && "need".equalsIgnoreCase(priority.trim());
    }
}
