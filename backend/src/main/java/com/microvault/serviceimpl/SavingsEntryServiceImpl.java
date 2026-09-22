package com.microvault.serviceimpl;

import com.microvault.dao.SavingsEntryDAO;
import com.microvault.dto.SavingsEntryDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.SavingsEntry;
import com.microvault.service.SavingsEntryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for savings entries. The DAO is received through the
 * constructor, so this class is bound to the SavingsEntryDAO interface and not
 * to a concrete class.
 */
public class SavingsEntryServiceImpl implements SavingsEntryService {

    private final SavingsEntryDAO savingsEntryDAO;

    public SavingsEntryServiceImpl(SavingsEntryDAO savingsEntryDAO) {
        this.savingsEntryDAO = savingsEntryDAO;
    }

    @Override
    public SavingsEntryDTO createSavingsEntry(SavingsEntry savingsEntry) {

        validateSavingsEntry(savingsEntry);

        SavingsEntry savedSavingsEntry = savingsEntryDAO.create(savingsEntry);
        return toDTO(savedSavingsEntry);
    }

    @Override
    public SavingsEntryDTO getSavingsEntryById(UUID id) {
        if (id == null) {
            throw new ValidationException("Savings entry id is required");
        }
        SavingsEntry savingsEntry = savingsEntryDAO.findById(id);
        if (savingsEntry == null) {
            throw new ValidationException("No active savings entry found with id " + id);
        }
        return toDTO(savingsEntry);
    }

    @Override
    public List<SavingsEntryDTO> getAllSavingsEntries() {
        return toDTOList(savingsEntryDAO.findAll());
    }

    @Override
    public List<SavingsEntryDTO> getSavingsEntriesByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(savingsEntryDAO.findByUserId(userId));
    }

    @Override
    public BigDecimal getTotalSavings(UUID userId) {

        if (userId == null) {
            throw new ValidationException("User id is required");
        }

        List<SavingsEntry> savingsEntries = savingsEntryDAO.findByUserId(userId);

        BigDecimal total = BigDecimal.ZERO;
        for (SavingsEntry savingsEntry : savingsEntries) {
            if (savingsEntry.getAmount() != null) {
                total = total.add(savingsEntry.getAmount());
            }
        }
        return total;
    }

    @Override
    public boolean updateSavingsEntry(SavingsEntry savingsEntry) {

        if (savingsEntry == null || savingsEntry.getId() == null) {
            throw new ValidationException("Savings entry id is required for an update");
        }
        validateSavingsEntry(savingsEntry);

        SavingsEntry existingSavingsEntry = savingsEntryDAO.findById(savingsEntry.getId());
        if (existingSavingsEntry == null) {
            throw new ValidationException("No active savings entry found with id " + savingsEntry.getId());
        }

        return savingsEntryDAO.update(savingsEntry);
    }

    @Override
    public boolean softDeleteSavingsEntry(UUID id) {
        if (id == null) {
            throw new ValidationException("Savings entry id is required");
        }
        if (savingsEntryDAO.findById(id) == null) {
            throw new ValidationException("No active savings entry found with id " + id);
        }
        return savingsEntryDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateSavingsEntry(SavingsEntry savingsEntry) {

        if (savingsEntry == null) {
            throw new ValidationException("Savings entry is required");
        }
        if (savingsEntry.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (savingsEntry.getTitle() == null || savingsEntry.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (savingsEntry.getCategory() == null || savingsEntry.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (savingsEntry.getAmount() == null) {
            throw new ValidationException("Amount is required");
        }
        if (savingsEntry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        if (savingsEntry.getEntryDate() == null) {
            throw new ValidationException("Entry date is required");
        }
        if (savingsEntry.getEntryDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Entry date cannot be in the future");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private SavingsEntryDTO toDTO(SavingsEntry savingsEntry) {

        SavingsEntryDTO savingsEntryDTO = new SavingsEntryDTO();
        savingsEntryDTO.setId(savingsEntry.getId());
        savingsEntryDTO.setUserId(savingsEntry.getUserId());
        savingsEntryDTO.setTitle(savingsEntry.getTitle());
        savingsEntryDTO.setCategory(savingsEntry.getCategory());
        savingsEntryDTO.setAmount(savingsEntry.getAmount());
        savingsEntryDTO.setEntryDate(savingsEntry.getEntryDate());
        savingsEntryDTO.setNote(savingsEntry.getNote());
        savingsEntryDTO.setCreatedAt(savingsEntry.getCreatedAt());
        return savingsEntryDTO;
    }

    private List<SavingsEntryDTO> toDTOList(List<SavingsEntry> savingsEntries) {
        List<SavingsEntryDTO> savingsEntryDTOs = new ArrayList<>();
        for (SavingsEntry savingsEntry : savingsEntries) {
            savingsEntryDTOs.add(toDTO(savingsEntry));
        }
        return savingsEntryDTOs;
    }
}
