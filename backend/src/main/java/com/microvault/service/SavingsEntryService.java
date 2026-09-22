package com.microvault.service;

import com.microvault.dto.SavingsEntryDTO;
import com.microvault.model.SavingsEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for savings entries: validation, lookups and the total
 * amount a user has put aside.
 */
public interface SavingsEntryService {

    SavingsEntryDTO createSavingsEntry(SavingsEntry savingsEntry);

    SavingsEntryDTO getSavingsEntryById(UUID id);

    List<SavingsEntryDTO> getAllSavingsEntries();

    List<SavingsEntryDTO> getSavingsEntriesByUserId(UUID userId);

    BigDecimal getTotalSavings(UUID userId);

    boolean updateSavingsEntry(SavingsEntry savingsEntry);

    boolean softDeleteSavingsEntry(UUID id);
}
