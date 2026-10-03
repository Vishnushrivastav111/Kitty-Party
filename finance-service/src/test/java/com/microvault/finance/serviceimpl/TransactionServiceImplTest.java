package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void shouldCreateTransaction() {
        TransactionRequest request = request();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse saved = transactionService.create(userId, request);

        assertEquals("Salary", saved.getName());
        assertEquals("income", saved.getType());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldUpdateTransaction() {
        Transaction existing = new Transaction();
        existing.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        existing.setUserId(userId);
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(existing.getId(), userId))
                .thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRequest request = request();
        request.setName("Bonus");
        TransactionResponse updated = transactionService.update(userId, existing.getId(), request);

        assertEquals("Bonus", updated.getName());
    }

    @Test
    void shouldRejectFutureDate() {
        TransactionRequest request = request();
        request.setDate(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> transactionService.create(userId, request));
    }

    @Test
    void shouldRejectUnknownTransaction() {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> transactionService.get(userId, id));
    }

    private TransactionRequest request() {
        TransactionRequest request = new TransactionRequest();
        request.setName("Salary");
        request.setCategory("Income");
        request.setType("income");
        request.setAmount(new BigDecimal("50000"));
        request.setDate(LocalDate.now());
        return request;
    }
}
