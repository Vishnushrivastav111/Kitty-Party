package com.microvault.serviceimpl;

import com.microvault.business.TransactionBO;
import com.microvault.dao.TransactionDAO;
import com.microvault.dto.TransactionDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Transaction;
import com.microvault.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for transactions. The DAO is received through the
 * constructor, so this class is bound to the TransactionDAO interface and not
 * to a concrete class.
 */
public class TransactionServiceImpl implements TransactionService {

    private final TransactionDAO transactionDAO;
    private final TransactionBO transactionBO = new TransactionBO();

    public TransactionServiceImpl(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    @Override
    public TransactionDTO createTransaction(Transaction transaction) {

        validateTransaction(transaction);

        Transaction savedTransaction = transactionDAO.create(transaction);
        return toDTO(savedTransaction);
    }

    @Override
    public TransactionDTO getTransactionById(UUID id) {
        if (id == null) {
            throw new ValidationException("Transaction id is required");
        }
        Transaction transaction = transactionDAO.findById(id);
        if (transaction == null) {
            throw new ValidationException("No active transaction found with id " + id);
        }
        return toDTO(transaction);
    }

    @Override
    public List<TransactionDTO> getAllTransactions() {
        return toDTOList(transactionDAO.findAll());
    }

    @Override
    public List<TransactionDTO> getTransactionsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(transactionDAO.findByUserId(userId));
    }

    @Override
    public List<TransactionDTO> getTransactionsByDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {

        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        if (fromDate == null || toDate == null) {
            throw new ValidationException("Both from date and to date are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ValidationException("From date cannot be after to date");
        }

        return toDTOList(transactionDAO.findByUserIdAndDateRange(userId, fromDate, toDate));
    }

    @Override
    public List<TransactionDTO> getTransactionsByCategory(UUID userId, String category) {

        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }

        return toDTOList(transactionDAO.findByUserIdAndCategory(userId, category));
    }

    @Override
    public BigDecimal getTotalIncome(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        List<Transaction> transactions = transactionDAO.findByUserId(userId);
        return transactionBO.calculateTotalIncome(transactions);
    }

    @Override
    public BigDecimal getTotalExpense(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        List<Transaction> transactions = transactionDAO.findByUserId(userId);
        return transactionBO.calculateTotalExpense(transactions);
    }

    @Override
    public boolean updateTransaction(Transaction transaction) {

        if (transaction == null || transaction.getId() == null) {
            throw new ValidationException("Transaction id is required for an update");
        }
        validateTransaction(transaction);

        Transaction existingTransaction = transactionDAO.findById(transaction.getId());
        if (existingTransaction == null) {
            throw new ValidationException("No active transaction found with id " + transaction.getId());
        }

        return transactionDAO.update(transaction);
    }

    @Override
    public boolean softDeleteTransaction(UUID id) {
        if (id == null) {
            throw new ValidationException("Transaction id is required");
        }
        if (transactionDAO.findById(id) == null) {
            throw new ValidationException("No active transaction found with id " + id);
        }
        return transactionDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new ValidationException("Transaction is required");
        }
        if (transaction.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (transaction.getName() == null || transaction.getName().trim().isEmpty()) {
            throw new ValidationException("Transaction name is required");
        }
        if (transaction.getCategory() == null || transaction.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (transaction.getType() == null || !transaction.getType().matches("income|expense")) {
            throw new ValidationException("Transaction type must be income or expense");
        }
        if (transaction.getAmount() == null) {
            throw new ValidationException("Amount is required");
        }
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        if (transaction.getTransactionDate() == null) {
            throw new ValidationException("Transaction date is required");
        }
        if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in the future");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private TransactionDTO toDTO(Transaction transaction) {

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setId(transaction.getId());
        transactionDTO.setUserId(transaction.getUserId());
        transactionDTO.setName(transaction.getName());
        transactionDTO.setCategory(transaction.getCategory());
        transactionDTO.setType(transaction.getType());
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setTransactionDate(transaction.getTransactionDate());
        transactionDTO.setNote(transaction.getNote());
        transactionDTO.setCreatedAt(transaction.getCreatedAt());
        return transactionDTO;
    }

    private List<TransactionDTO> toDTOList(List<Transaction> transactions) {
        List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (Transaction transaction : transactions) {
            transactionDTOs.add(toDTO(transaction));
        }
        return transactionDTOs;
    }
}
