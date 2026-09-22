package com.microvault.business;

import com.microvault.model.Report;
import com.microvault.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Builds the numbers of a financial report from the transactions of a period.
 */
public class ReportBO {

    private final TransactionBO transactionBO = new TransactionBO();

    /**
     * Fills income, expense, net amount and the transaction count of a report
     * using the transactions that belong to the report period.
     */
    public Report calculateTotals(Report report, List<Transaction> transactions) {

        if (report == null) {
            return null;
        }

        BigDecimal totalIncome = transactionBO.calculateTotalIncome(transactions);
        BigDecimal totalExpense = transactionBO.calculateTotalExpense(transactions);

        report.setTotalIncome(totalIncome);
        report.setTotalExpense(totalExpense);
        report.setNetAmount(totalIncome.subtract(totalExpense));
        report.setTransactionCount(transactions == null ? 0 : transactions.size());

        return report;
    }

    /** A report period must be complete, in the right order and not in the future. */
    public boolean isValidDateRange(LocalDate fromDate, LocalDate toDate, LocalDate today) {

        if (fromDate == null || toDate == null) {
            return false;
        }
        if (fromDate.isAfter(toDate)) {
            return false;
        }
        if (today != null && toDate.isAfter(today)) {
            return false;
        }
        return true;
    }

    /** Savings rate in percent: (income - expense) / income * 100. */
    public BigDecimal calculateSavingsRate(BigDecimal totalIncome, BigDecimal totalExpense) {

        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal expense = totalExpense == null ? BigDecimal.ZERO : totalExpense;
        BigDecimal saved = totalIncome.subtract(expense);

        return saved.multiply(new BigDecimal("100"))
                .divide(totalIncome, 2, java.math.RoundingMode.HALF_UP);
    }
}
