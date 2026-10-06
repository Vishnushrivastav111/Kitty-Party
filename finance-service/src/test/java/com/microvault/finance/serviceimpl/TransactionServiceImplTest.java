package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    void createPersistsNormalisedTransaction() {
        TransactionRequest request = request();
        request.setName("  Salary ");
        request.setCategory(" Income ");
        request.setType("  INCOME ");
        request.setNote("October");

        TransactionResponse response = transactionService.create(userId, request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("Salary", saved.getName());
        assertEquals("Income", saved.getCategory());
        assertEquals("income", saved.getType());
        assertEquals(new BigDecimal("50000"), saved.getAmount());
        assertEquals("October", saved.getNote());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        assertEquals(saved.getId(), response.getId());
        assertEquals("Salary", response.getName());
        assertEquals("Income", response.getCategory());
        assertEquals("income", response.getType());
        assertEquals(request.getDate(), response.getDate());
        assertEquals("October", response.getNote());
    }

    @Test
    void createAcceptsExpenseType() {
        TransactionRequest request = request();
        request.setType("Expense");

        assertEquals("expense", transactionService.create(userId, request).getType());
    }

    @Test
    void createRejectsFutureDate() {
        TransactionRequest request = request();
        request.setDate(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> transactionService.create(userId, request));

        assertEquals("Date cannot be in the future", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownType() {
        TransactionRequest request = request();
        request.setType("transfer");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> transactionService.create(userId, request));

        assertEquals("Type must be income or expense", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsNullType() {
        TransactionRequest request = request();
        request.setType(null);

        assertThrows(ValidationException.class, () -> transactionService.create(userId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsBlankType() {
        TransactionRequest request = request();
        request.setType("   ");

        assertThrows(ValidationException.class, () -> transactionService.create(userId, request));
    }

    @Test
    void createPropagatesRepositoryException() {
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new IllegalStateException("db down"));

        TransactionRequest request = request();
        assertThrows(IllegalStateException.class, () -> transactionService.create(userId, request));
    }

    // ---------------------------------------------------------------- update

    @Test
    void updateChangesExistingTransaction() {
        Transaction existing = stored("Salary", "income");
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        TransactionRequest request = request();
        request.setName("Bonus");

        TransactionResponse updated = transactionService.update(userId, id, request);

        assertEquals("Bonus", updated.getName());
        assertEquals(id, updated.getId());
        assertEquals("Bonus", existing.getName());
        assertNotNull(existing.getUpdatedAt());
        verify(transactionRepository).save(existing);
    }

    @Test
    void updateOfUnknownTransactionThrowsNotFound() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        TransactionRequest request = request();
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> transactionService.update(userId, id, request));

        assertEquals("Transaction not found", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateRejectsFutureDateBeforeLookingUpTheRecord() {
        TransactionRequest request = request();
        request.setDate(LocalDate.now().plusDays(3));

        assertThrows(ValidationException.class, () -> transactionService.update(userId, id, request));
        verify(transactionRepository, never()).findByIdAndUserIdAndDeletedFalse(any(), any());
    }

    @Test
    void updateRejectsInvalidType() {
        TransactionRequest request = request();
        request.setType("gift");

        assertThrows(ValidationException.class, () -> transactionService.update(userId, id, request));
    }

    // ---------------------------------------------------------------- get / list

    @Test
    void getReturnsTransaction() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId))
                .thenReturn(Optional.of(stored("Rent", "expense")));

        TransactionResponse response = transactionService.get(userId, id);

        assertEquals("Rent", response.getName());
        assertEquals("expense", response.getType());
        assertEquals(new BigDecimal("1200"), response.getAmount());
    }

    @Test
    void getOfUnknownTransactionThrowsNotFound() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.get(userId, id));
    }

    @Test
    void listReturnsAllTransactionsInRepositoryOrder() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(stored("Rent", "expense"), stored("Salary", "income")));

        List<TransactionResponse> rows = transactionService.list(userId);

        assertEquals(2, rows.size());
        assertEquals("Rent", rows.get(0).getName());
        assertEquals("Salary", rows.get(1).getName());
    }

    @Test
    void listReturnsEmptyListWhenUserHasNoTransactions() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        assertTrue(transactionService.list(userId).isEmpty());
    }

    @Test
    void listPropagatesRepositoryException() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> transactionService.list(userId));
    }

    // ---------------------------------------------------------------- delete

    @Test
    void softDeleteMarksTransactionDeleted() {
        Transaction existing = stored("Rent", "expense");
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        transactionService.softDelete(userId, id);

        assertTrue(existing.isDeleted());
        assertNotNull(existing.getDeletedAt());
        assertNotNull(existing.getUpdatedAt());
        verify(transactionRepository).save(existing);
    }

    @Test
    void softDeleteOfUnknownTransactionThrowsNotFound() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.softDelete(userId, id));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryTransactionDeleted() {
        Transaction first = stored("Rent", "expense");
        Transaction second = stored("Salary", "income");
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(first, second));

        transactionService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void softDeleteAllWithNoTransactionsSavesNothing() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        transactionService.softDeleteAll(userId);

        verify(transactionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private TransactionRequest request() {
        TransactionRequest request = new TransactionRequest();
        request.setName("Salary");
        request.setCategory("Income");
        request.setType("income");
        request.setAmount(new BigDecimal("50000"));
        request.setDate(LocalDate.now());
        return request;
    }

    private Transaction stored(String name, String type) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUserId(userId);
        transaction.setName(name);
        transaction.setCategory("General");
        transaction.setType(type);
        transaction.setAmount(new BigDecimal("1200"));
        transaction.setDate(LocalDate.of(2026, 9, 1));
        return transaction;
    }
}
