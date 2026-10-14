package com.microvault.finance.serviceimpl;

import com.microvault.finance.entity.Transaction;
import com.microvault.finance.service.FinancialPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Monthly consumption for one budget category. */
public final class BudgetSpending {

    private BudgetSpending() {
    }

    public static BigDecimal spent(List<Transaction> transactions, String category, LocalDate today) {
        if (category == null || category.isBlank() || FinancialPlan.nonConsumptionCategory(category)) {
            return FinancialPlan.money(null);
        }
        YearMonth month = YearMonth.from(today == null ? LocalDate.now() : today);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getDate() == null || transaction.getAmount() == null) {
                continue;
            }
            if (transaction.getDate().isBefore(start) || transaction.getDate().isAfter(end)) {
                continue;
            }
            if (!category.equalsIgnoreCase(transaction.getCategory())) {
                continue;
            }
            String type = transaction.getType() == null ? "" : transaction.getType().trim().toLowerCase();
            if ("expense".equals(type)) {
                total = total.add(transaction.getAmount());
            } else if ("refund".equals(type)) {
                total = total.subtract(transaction.getAmount());
            }
        }
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }
        return FinancialPlan.money(total);
    }
}
