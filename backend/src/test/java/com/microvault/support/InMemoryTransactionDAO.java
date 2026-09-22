package com.microvault.support;

import com.microvault.dao.TransactionDAO;
import com.microvault.model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryTransactionDAO implements TransactionDAO {

    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction create(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setIsDeleted(Boolean.FALSE);
        transactions.add(copy(transaction));
        return copy(transaction);
    }

    @Override
    public Transaction findById(UUID id) {
        for (Transaction transaction : transactions) {
            if (id.equals(transaction.getId()) && !Boolean.TRUE.equals(transaction.getIsDeleted())) {
                return copy(transaction);
            }
        }
        return null;
    }

    @Override
    public List<Transaction> findAll() {
        return findMatching(null, null, null, null);
    }

    @Override
    public List<Transaction> findByUserId(UUID userId) {
        return findMatching(userId, null, null, null);
    }

    @Override
    public List<Transaction> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : findMatching(userId, null, null, null)) {
            LocalDate date = transaction.getTransactionDate();
            if (date != null && !date.isBefore(fromDate) && !date.isAfter(toDate)) {
                result.add(transaction);
            }
        }
        return result;
    }

    @Override
    public List<Transaction> findByUserIdAndCategory(UUID userId, String category) {
        return findMatching(userId, category, null, null);
    }

    @Override
    public List<Transaction> findByUserIdAndType(UUID userId, String type) {
        return findMatching(userId, null, type, null);
    }

    @Override
    public boolean update(Transaction transaction) {
        Transaction existing = findStored(transaction.getId());
        if (existing == null) {
            return false;
        }
        existing.setName(transaction.getName());
        existing.setCategory(transaction.getCategory());
        existing.setType(transaction.getType());
        existing.setAmount(transaction.getAmount());
        existing.setTransactionDate(transaction.getTransactionDate());
        existing.setNote(transaction.getNote());
        existing.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        Transaction existing = findStored(id);
        if (existing == null) {
            return false;
        }
        existing.setIsDeleted(Boolean.TRUE);
        existing.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private List<Transaction> findMatching(UUID userId, String category, String type, UUID ignored) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (Boolean.TRUE.equals(transaction.getIsDeleted())) {
                continue;
            }
            if (userId != null && !userId.equals(transaction.getUserId())) {
                continue;
            }
            if (category != null && !category.equals(transaction.getCategory())) {
                continue;
            }
            if (type != null && !type.equals(transaction.getType())) {
                continue;
            }
            result.add(copy(transaction));
        }
        return result;
    }

    private Transaction findStored(UUID id) {
        for (Transaction transaction : transactions) {
            if (id.equals(transaction.getId()) && !Boolean.TRUE.equals(transaction.getIsDeleted())) {
                return transaction;
            }
        }
        return null;
    }

    private Transaction copy(Transaction source) {
        Transaction transaction = new Transaction();
        transaction.setId(source.getId());
        transaction.setUserId(source.getUserId());
        transaction.setName(source.getName());
        transaction.setCategory(source.getCategory());
        transaction.setType(source.getType());
        transaction.setAmount(source.getAmount());
        transaction.setTransactionDate(source.getTransactionDate());
        transaction.setNote(source.getNote());
        transaction.setCreatedAt(source.getCreatedAt());
        transaction.setUpdatedAt(source.getUpdatedAt());
        transaction.setIsDeleted(source.getIsDeleted());
        transaction.setDeletedAt(source.getDeletedAt());
        return transaction;
    }
}
