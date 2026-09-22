package com.microvault.dao;

import com.microvault.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "transactions" table. Only data access is
 * described here, no business rules.
 */
public interface TransactionDAO {

    Transaction create(Transaction transaction);

    Transaction findById(UUID id);

    List<Transaction> findAll();

    List<Transaction> findByUserId(UUID userId);

    List<Transaction> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate);

    List<Transaction> findByUserIdAndCategory(UUID userId, String category);

    List<Transaction> findByUserIdAndType(UUID userId, String type);

    boolean update(Transaction transaction);

    boolean softDelete(UUID id);
}
