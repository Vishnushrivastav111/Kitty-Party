package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.DuplicateResourceException;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    void createPersistsBudgetWithSpentFromExpenseTransactions() {
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "Food")).thenReturn(Optional.empty());
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(
                        tx("expense", "food", "100"),
                        tx("expense", "FOOD", "50"),
                        tx("income", "Food", "999"),
                        tx("expense", "Travel", "999"),
                        tx("expense", "Food", null),
                        tx(null, "Food", "999")));
        BudgetRequest request = request("  Food ", "1000", "monthly");

        BudgetResponse response = budgetService.create(userId, request);

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        Budget saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("Food", saved.getCategory());
        assertEquals(new BigDecimal("1000"), saved.getLimit());
        assertEquals("monthly", saved.getNote());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());

        assertEquals(saved.getId(), response.getId());
        assertEquals("Food", response.getCategory());
        assertEquals(new BigDecimal("1000"), response.getLimit());
        assertEquals("monthly", response.getNote());
        assertEquals(new BigDecimal("150"), response.getSpent());
        assertEquals(new BigDecimal("850"), response.getRemaining());
        assertEquals(new BigDecimal("15.00"), response.getUsedPercent());
        assertEquals("Within budget", response.getUsage());
    }

    @Test
    void createRejectsDuplicateCategoryIgnoringCase() {
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "food"))
                .thenReturn(Optional.of(stored("Food", "1000")));

        BudgetRequest request = request("food", "500", null);
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> budgetService.create(userId, request));

        assertEquals("A budget already exists for food", exception.getMessage());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createPropagatesRepositoryException() {
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "Food")).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenThrow(new IllegalStateException("db down"));

        BudgetRequest request = request("Food", "500", null);
        assertThrows(IllegalStateException.class, () -> budgetService.create(userId, request));
    }

    // ---------------------------------------------------------------- update

    @Test
    void updateChangesBudgetWhenCategoryIsFree() {
        Budget existing = stored("Food", "1000");
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "Dining")).thenReturn(Optional.empty());
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        BudgetResponse response = budgetService.update(userId, id, request("Dining", "2000", "new"));

        assertEquals("Dining", existing.getCategory());
        assertEquals(new BigDecimal("2000"), existing.getLimit());
        assertEquals("new", existing.getNote());
        assertEquals("Dining", response.getCategory());
        assertEquals(BigDecimal.ZERO, response.getSpent());
        assertNotNull(existing.getUpdatedAt());
        verify(budgetRepository).save(existing);
    }

    @Test
    void updateAllowsKeepingTheSameCategoryOnTheSameBudget() {
        Budget existing = stored("Food", "1000");
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "Food")).thenReturn(Optional.of(existing));
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        BudgetResponse response = budgetService.update(userId, id, request("Food", "3000", null));

        assertEquals(new BigDecimal("3000"), response.getLimit());
    }

    @Test
    void updateRejectsCategoryUsedByAnotherBudget() {
        Budget existing = stored("Food", "1000");
        Budget other = stored("Travel", "500");
        other.setId(UUID.randomUUID());
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        when(budgetRepository.findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, "Travel")).thenReturn(Optional.of(other));

        BudgetRequest request = request("Travel", "700", null);
        assertThrows(DuplicateResourceException.class, () -> budgetService.update(userId, id, request));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateOfUnknownBudgetThrowsNotFound() {
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        BudgetRequest request = request("Food", "700", null);
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> budgetService.update(userId, id, request));

        assertEquals("Budget not found", exception.getMessage());
    }

    // ---------------------------------------------------------------- list / usage labels

    @Test
    void listReturnsEmptyWhenNoBudgets() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of());

        assertTrue(budgetService.list(userId).isEmpty());
    }

    @Test
    void listComputesUsageLabelsForEveryBand() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(
                        tx("expense", "Low", "79"),
                        tx("expense", "Edge80", "80"),
                        tx("expense", "Edge100", "100"),
                        tx("expense", "Over", "150")));
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId))
                .thenReturn(List.of(
                        stored("Low", "100"),
                        stored("Edge80", "100"),
                        stored("Edge100", "100"),
                        stored("Over", "100")));

        List<BudgetResponse> rows = budgetService.list(userId);

        assertEquals(4, rows.size());
        assertEquals("Within budget", rows.get(0).getUsage());
        assertEquals(new BigDecimal("79.00"), rows.get(0).getUsedPercent());
        assertEquals(new BigDecimal("21"), rows.get(0).getRemaining());
        assertEquals("Close to limit", rows.get(1).getUsage());
        assertEquals("Close to limit", rows.get(2).getUsage());
        assertEquals(new BigDecimal("100.00"), rows.get(2).getUsedPercent());
        assertEquals("Over budget", rows.get(3).getUsage());
        assertEquals(new BigDecimal("150.00"), rows.get(3).getUsedPercent());
        assertEquals(BigDecimal.ZERO, rows.get(3).getRemaining());
    }

    @Test
    void listHandlesBudgetWithoutLimitOrCategory() {
        Budget noLimit = stored("Misc", "1");
        noLimit.setLimit(null);
        Budget noCategory = stored("x", "100");
        noCategory.setCategory(null);
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(tx("expense", "Misc", "40"), tx("expense", null, "5")));
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(noLimit, noCategory));

        List<BudgetResponse> rows = budgetService.list(userId);

        assertEquals(BigDecimal.ZERO, rows.get(0).getLimit());
        assertEquals(new BigDecimal("40"), rows.get(0).getSpent());
        assertEquals(BigDecimal.ZERO, rows.get(0).getUsedPercent());
        assertEquals(BigDecimal.ZERO, rows.get(0).getRemaining());
        assertEquals("Within budget", rows.get(0).getUsage());
        assertEquals(BigDecimal.ZERO, rows.get(1).getSpent());
        assertEquals(new BigDecimal("100"), rows.get(1).getRemaining());
    }

    @Test
    void listPropagatesRepositoryException() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> budgetService.list(userId));
    }

    // ---------------------------------------------------------------- delete

    @Test
    void softDeleteMarksBudgetDeleted() {
        Budget existing = stored("Food", "100");
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        budgetService.softDelete(userId, id);

        assertTrue(existing.isDeleted());
        assertNotNull(existing.getDeletedAt());
        verify(budgetRepository).save(existing);
    }

    @Test
    void softDeleteOfUnknownBudgetThrowsNotFound() {
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.softDelete(userId, id));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryBudgetDeleted() {
        Budget first = stored("Food", "100");
        Budget second = stored("Travel", "100");
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(first, second));

        budgetService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(budgetRepository, times(2)).save(any(Budget.class));
    }

    @Test
    void softDeleteAllWithNoBudgetsSavesNothing() {
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of());

        budgetService.softDeleteAll(userId);

        verify(budgetRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private BudgetRequest request(String category, String limit, String note) {
        BudgetRequest request = new BudgetRequest();
        request.setCategory(category);
        request.setLimit(new BigDecimal(limit));
        request.setNote(note);
        return request;
    }

    private Budget stored(String category, String limit) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setLimit(new BigDecimal(limit));
        return budget;
    }

    private Transaction tx(String type, String category, String amount) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setAmount(amount == null ? null : new BigDecimal(amount));
        return transaction;
    }
}
