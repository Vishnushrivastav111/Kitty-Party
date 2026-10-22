package com.microvault.finance.serviceimpl;

import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.BudgetNotification;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.repository.BudgetNotificationRepository;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BudgetNotificationRepository notificationRepository;

    private BudgetAlertService alerts;
    private final UUID userId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @BeforeEach
    void setUp() {
        alerts = new BudgetAlertService(budgetRepository, transactionRepository, notificationRepository);
    }

    @Test
    void spendingBelowEveryThresholdStoresNothing() {
        Budget food = budget("Food", "100", UUID.randomUUID());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "24")));

        alerts.evaluate(userId);

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void aZeroOrMissingLimitIsSkipped() {
        Budget zero = budget("Food", "0", UUID.randomUUID());
        Budget negative = budget("Travel", "-5", UUID.randomUUID());
        Budget missing = budget("Rent", "1", UUID.randomUUID());
        missing.setLimit(null);
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId))
                .thenReturn(List.of(zero, negative, missing));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "500")));

        alerts.evaluate(userId);

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void crossingEachThresholdStoresTheMatchingTitleAndType() {
        Budget food = budget("Food", "100", UUID.randomUUID());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "100")));
        when(notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(any(), any())).thenReturn(false);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        alerts.evaluate(userId);

        ArgumentCaptor<BudgetNotification> captor = ArgumentCaptor.forClass(BudgetNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(4)).saveAndFlush(captor.capture());
        List<BudgetNotification> stored = captor.getAllValues();
        assertEquals("Food budget update", stored.get(0).getTitle());
        assertEquals("info", stored.get(0).getType());
        assertEquals("Food budget halfway", stored.get(1).getTitle());
        assertEquals("info", stored.get(1).getType());
        assertEquals("Food budget warning", stored.get(2).getTitle());
        assertEquals("warning", stored.get(2).getType());
        assertEquals("Food budget exhausted", stored.get(3).getTitle());
        assertEquals("danger", stored.get(3).getType());
        assertEquals(userId, stored.get(3).getUserId());
        assertEquals(food.getId(), stored.get(3).getBudgetId());
        assertFalse(stored.get(3).isRead());
        assertFalse(stored.get(3).isDismissed());
        assertFalse(stored.get(3).isDeleted());
        assertTrue(stored.get(3).getMessage().contains("₹100.00"));
        assertTrue(stored.get(3).getMessage().contains("100% threshold"));
        assertTrue(stored.get(3).getReferenceKey().contains(":100"));
    }

    @Test
    void halfwaySpendingStopsBeforeTheWarningAndExhaustedAlerts() {
        Budget food = budget("Food", "100", UUID.randomUUID());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "50")));
        when(notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(any(), any())).thenReturn(false);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        alerts.evaluate(userId);

        ArgumentCaptor<BudgetNotification> captor = ArgumentCaptor.forClass(BudgetNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        assertEquals("Food budget update", captor.getAllValues().get(0).getTitle());
        assertEquals("Food budget halfway", captor.getAllValues().get(1).getTitle());
    }

    @Test
    void anAlertThatAlreadyExistsIsNotStoredAgain() {
        Budget food = budget("Food", "100", UUID.randomUUID());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "80")));
        when(notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(any(), any())).thenReturn(true);

        alerts.evaluate(userId);

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void aDuplicateInsertIsIgnored() {
        Budget food = budget("Food", "100", UUID.randomUUID());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense("Food", "25")));
        when(notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(any(), any())).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate")).when(notificationRepository).saveAndFlush(any());

        alerts.evaluate(userId);

        verify(notificationRepository).saveAndFlush(any());
    }

    @Test
    void noBudgetsMeansNoAlerts() {
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        alerts.evaluate(userId);

        verify(notificationRepository, never()).existsByUserIdAndReferenceKeyAndDeletedFalse(any(), any());
    }

    private Budget budget(String category, String limit, UUID id) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setLimit(new BigDecimal(limit));
        return budget;
    }

    private Transaction expense(String category, String amount) {
        Transaction transaction = new Transaction();
        transaction.setCategory(category);
        transaction.setType("expense");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(LocalDate.now());
        return transaction;
    }
}
