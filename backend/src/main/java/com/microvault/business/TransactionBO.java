package com.microvault.business;

import com.microvault.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totals calculated from a list of transactions.
 */
public class TransactionBO {

    public BigDecimal calculateTotalByType(List<Transaction> transactions, String type) {

        BigDecimal total = BigDecimal.ZERO;
        if (transactions == null || type == null) {
            return total;
        }

        for (Transaction transaction : transactions) {
            if (type.equalsIgnoreCase(transaction.getType()) && transaction.getAmount() != null) {
                total = total.add(transaction.getAmount());
            }
        }
        return total;
    }

    public BigDecimal calculateTotalIncome(List<Transaction> transactions) {
        return calculateTotalByType(transactions, "income");
    }

    public BigDecimal calculateTotalExpense(List<Transaction> transactions) {
        return calculateTotalByType(transactions, "expense");
    }

    /** Income minus expense for the given list. */
    public BigDecimal calculateNetAmount(List<Transaction> transactions) {
        return calculateTotalIncome(transactions).subtract(calculateTotalExpense(transactions));
    }

    /** Total spent in one category, used by the budget screens. */
    public BigDecimal calculateSpentInCategory(List<Transaction> transactions, String category) {

        BigDecimal total = BigDecimal.ZERO;
        if (transactions == null || category == null) {
            return total;
        }

        for (Transaction transaction : transactions) {
            boolean isExpense = "expense".equalsIgnoreCase(transaction.getType());
            boolean sameCategory = category.equalsIgnoreCase(transaction.getCategory());
            if (isExpense && sameCategory && transaction.getAmount() != null) {
                total = total.add(transaction.getAmount());
            }
        }
        return total;
    }
}
