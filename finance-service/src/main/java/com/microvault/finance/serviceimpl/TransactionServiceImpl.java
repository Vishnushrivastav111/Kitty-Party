package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        validate(request);
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        copy(request, transaction);
        LocalDateTime now = LocalDateTime.now();
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        transaction.setDeleted(false);
        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        validate(request);
        Transaction transaction = load(userId, id);
        copy(request, transaction);
        transaction.setUpdatedAt(LocalDateTime.now());
        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID id) {
        return toResponse(load(userId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> list(UUID userId) {
        List<TransactionResponse> rows = new ArrayList<>();
        for (Transaction transaction : transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)) {
            rows.add(toResponse(transaction));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        Transaction transaction = load(userId, id);
        markDeleted(transaction);
        transactionRepository.save(transaction);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        for (Transaction transaction : transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)) {
            markDeleted(transaction);
            transactionRepository.save(transaction);
        }
    }

    private void validate(TransactionRequest request) {
        String type = request.getType() == null ? "" : request.getType().trim().toLowerCase();
        if (!"income".equals(type) && !"expense".equals(type)) {
            throw new ValidationException("Type must be income or expense");
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future");
        }
    }

    private void copy(TransactionRequest request, Transaction transaction) {
        transaction.setName(request.getName().trim());
        transaction.setCategory(request.getCategory().trim());
        transaction.setType(request.getType().trim().toLowerCase());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNote(request.getNote());
    }

    private Transaction load(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private void markDeleted(Transaction transaction) {
        LocalDateTime now = LocalDateTime.now();
        transaction.setDeleted(true);
        transaction.setDeletedAt(now);
        transaction.setUpdatedAt(now);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setName(transaction.getName());
        response.setCategory(transaction.getCategory());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getDate());
        response.setNote(transaction.getNote());
        return response;
    }
}
