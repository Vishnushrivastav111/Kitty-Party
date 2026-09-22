package com.microvault.service;

import com.microvault.dto.TransactionDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Transaction;
import com.microvault.serviceimpl.TransactionServiceImpl;
import com.microvault.support.InMemoryTransactionDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceTest {

    private TransactionService transactionService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(new InMemoryTransactionDAO());
        userId = UUID.randomUUID();
    }

    @Test
    void createValidTransaction() {
        TransactionDTO saved = transactionService.createTransaction(validExpense("Grocery", "4200"));

        assertNotNull(saved.getId());
        assertEquals("Grocery", saved.getName());
        assertEquals(new BigDecimal("4200"), saved.getAmount());
    }

    @Test
    void findTransaction() {
        TransactionDTO saved = transactionService.createTransaction(validExpense("Metro", "1500"));

        TransactionDTO found = transactionService.getTransactionById(saved.getId());

        assertEquals(saved.getName(), found.getName());
    }

    @Test
    void updateTransaction() {
        TransactionDTO saved = transactionService.createTransaction(validExpense("Taxi", "300"));
        Transaction update = validExpense("Taxi ride", "350");
        update.setId(saved.getId());

        assertTrue(transactionService.updateTransaction(update));
        assertEquals("Taxi ride", transactionService.getTransactionById(saved.getId()).getName());
    }

    @Test
    void findTransactionsByUser() {
        transactionService.createTransaction(validExpense("Food", "200"));
        transactionService.createTransaction(validIncome("Salary", "50000"));

        List<TransactionDTO> transactions = transactionService.getTransactionsByUserId(userId);

        assertEquals(2, transactions.size());
    }

    @Test
    void softDeleteTransaction() {
        TransactionDTO saved = transactionService.createTransaction(validExpense("Snack", "80"));

        assertTrue(transactionService.softDeleteTransaction(saved.getId()));
        assertThrows(ValidationException.class, () -> transactionService.getTransactionById(saved.getId()));
    }

    @Test
    void nullAmountIsRejected() {
        Transaction transaction = validExpense("No amount", "100");
        transaction.setAmount(null);

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(transaction));
    }

    @Test
    void negativeAmountIsRejected() {
        Transaction transaction = validExpense("Negative", "-10");

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(transaction));
    }

    @Test
    void nullCategoryIsRejected() {
        Transaction transaction = validExpense("No category", "100");
        transaction.setCategory(null);

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(transaction));
    }

    @Test
    void futureDateIsRejected() {
        Transaction transaction = validExpense("Future", "100");
        transaction.setTransactionDate(LocalDate.now().plusDays(2));

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(transaction));
    }

    @Test
    void missingTransactionIsNotFound() {
        assertThrows(ValidationException.class, () -> transactionService.getTransactionById(UUID.randomUUID()));
    }

    private Transaction validExpense(String name, String amount) {
        return new Transaction(userId, name, "Food", "expense", new BigDecimal(amount), LocalDate.now(), null);
    }

    private Transaction validIncome(String name, String amount) {
        return new Transaction(userId, name, "Salary", "income", new BigDecimal(amount), LocalDate.now(), null);
    }
}
