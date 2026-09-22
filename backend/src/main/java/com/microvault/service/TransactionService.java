package com.microvault.service;

import com.microvault.dto.TransactionDTO;
import com.microvault.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for transactions: validation, lookups and the income
 * and expense totals shown on the dashboard.
 */
public interface TransactionService {

    TransactionDTO createTransaction(Transaction transaction);

    TransactionDTO getTransactionById(UUID id);

    List<TransactionDTO> getAllTransactions();

    List<TransactionDTO> getTransactionsByUserId(UUID userId);

    List<TransactionDTO> getTransactionsByDateRange(UUID userId, LocalDate fromDate, LocalDate toDate);

    List<TransactionDTO> getTransactionsByCategory(UUID userId, String category);

    BigDecimal getTotalIncome(UUID userId);

    BigDecimal getTotalExpense(UUID userId);

    boolean updateTransaction(Transaction transaction);

    boolean softDeleteTransaction(UUID id);
}
