package com.microvault.dao;

import com.microvault.model.SavingsEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "savings_entries" table. Only data access is
 * described here, no business rules.
 */
public interface SavingsEntryDAO {

    SavingsEntry create(SavingsEntry savingsEntry);

    SavingsEntry findById(UUID id);

    List<SavingsEntry> findAll();

    List<SavingsEntry> findByUserId(UUID userId);

    List<SavingsEntry> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate);

    boolean update(SavingsEntry savingsEntry);

    boolean softDelete(UUID id);
}
