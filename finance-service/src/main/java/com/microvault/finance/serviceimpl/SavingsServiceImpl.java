package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.SavingsRepository;
import com.microvault.finance.service.SavingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SavingsServiceImpl implements SavingsService {

    private final SavingsRepository savingsRepository;

    public SavingsServiceImpl(SavingsRepository savingsRepository) {
        this.savingsRepository = savingsRepository;
    }

    @Override
    public SavingsResponse create(UUID userId, SavingsRequest request) {
        validate(request);
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        copy(request, entry);
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setDeleted(false);
        return toResponse(savingsRepository.save(entry));
    }

    @Override
    public SavingsResponse update(UUID userId, UUID id, SavingsRequest request) {
        validate(request);
        SavingsEntry entry = load(userId, id);
        copy(request, entry);
        entry.setUpdatedAt(LocalDateTime.now());
        return toResponse(savingsRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsResponse> list(UUID userId) {
        List<SavingsResponse> rows = new ArrayList<>();
        for (SavingsEntry entry : savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)) {
            rows.add(toResponse(entry));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        SavingsEntry entry = load(userId, id);
        markDeleted(entry);
        savingsRepository.save(entry);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        for (SavingsEntry entry : savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)) {
            markDeleted(entry);
            savingsRepository.save(entry);
        }
    }

    private void validate(SavingsRequest request) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future");
        }
    }

    private void copy(SavingsRequest request, SavingsEntry entry) {
        entry.setTitle(request.getTitle().trim());
        entry.setCategory(request.getCategory());
        entry.setAmount(request.getAmount());
        entry.setDate(request.getDate());
        entry.setNote(request.getNote());
    }

    private SavingsEntry load(UUID userId, UUID id) {
        return savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings entry not found"));
    }

    private void markDeleted(SavingsEntry entry) {
        LocalDateTime now = LocalDateTime.now();
        entry.setDeleted(true);
        entry.setDeletedAt(now);
        entry.setUpdatedAt(now);
    }

    private SavingsResponse toResponse(SavingsEntry entry) {
        SavingsResponse response = new SavingsResponse();
        response.setId(entry.getId());
        response.setTitle(entry.getTitle());
        response.setCategory(entry.getCategory());
        response.setAmount(entry.getAmount());
        response.setDate(entry.getDate());
        response.setNote(entry.getNote());
        return response;
    }
}
