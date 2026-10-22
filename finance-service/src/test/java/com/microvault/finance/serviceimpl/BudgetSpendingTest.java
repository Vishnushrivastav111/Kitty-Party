package com.microvault.finance.serviceimpl;

import com.microvault.finance.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetSpendingTest {

    private final LocalDate today = LocalDate.of(2026, 10, 22);

    @ParameterizedTest
    @CsvSource({
            "transfer,0.00",
            "TRANSFER,0.00",
            " savings ,0.00",
            "Investment,0.00",
            "'',0.00",
            "'   ',0.00"
    })
    void nonConsumptionAndBlankCategoriesSpendNothing(String category, String expected) {
        assertEquals(new BigDecimal(expected),
                BudgetSpending.spent(List.of(expense("Food", "25", today)), category, today));
    }

    @Test
    void aMissingCategoryOrDayUsesZeroOrTheCurrentMonth() {
        assertEquals(new BigDecimal("0.00"), BudgetSpending.spent(List.of(expense("Food", "25", today)), null, today));
        Transaction current = expense("Food", "15", LocalDate.now());
        assertEquals(new BigDecimal("15.00"), BudgetSpending.spent(List.of(current), "Food", null));
    }

    @Test
    void onlyThisMonthsExpensesAndRefundsInTheCategoryCount() {
        List<Transaction> rows = List.of(
                tx("expense", "Food", "9", null),
                tx("expense", "Food", null, today),
                tx("expense", "Food", "10", today.minusMonths(1).withDayOfMonth(28)),
                tx("expense", "Food", "4", today.plusMonths(1).withDayOfMonth(1)),
                tx("expense", "Food", "10", today.withDayOfMonth(1)),
                tx("expense", "Food", "5", today.withDayOfMonth(today.lengthOfMonth())),
                tx(" EXPENSE ", "food", "7", today),
                tx("expense", "Travel", "100", today),
                tx("income", "Food", "50", today),
                tx("refund", "Food", "3", today),
                tx(" Refund ", "Food", "1", today),
                tx(null, "Food", "8", today),
                tx("transfer", "Food", "6", today)
        );

        assertEquals(new BigDecimal("18.00"), BudgetSpending.spent(rows, "Food", today));
    }

    @Test
    void refundsCannotPushTheMonthBelowZero() {
        List<Transaction> rows = List.of(
                expense("Food", "10", today),
                tx("refund", "Food", "40", today)
        );

        assertEquals(new BigDecimal("0.00"), BudgetSpending.spent(rows, "Food", today));
    }

    private static Transaction expense(String category, String amount, LocalDate date) {
        return tx("expense", category, amount, date);
    }

    private static Transaction tx(String type, String category, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setAmount(amount == null ? null : new BigDecimal(amount));
        transaction.setDate(date);
        return transaction;
    }
}
