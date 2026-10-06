package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.GoalRepository;
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
class GoalServiceImplTest {

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private GoalServiceImpl goalService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    void createPersistsActiveGoal() {
        GoalRequest request = request("  Laptop ", "1000", "250", "active");
        request.setCategory("Tech");
        request.setDeadline(LocalDate.of(2027, 1, 1));

        GoalResponse response = goalService.create(userId, request);

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        Goal saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("Laptop", saved.getTitle());
        assertEquals("Tech", saved.getCategory());
        assertEquals(new BigDecimal("1000"), saved.getTarget());
        assertEquals(new BigDecimal("250"), saved.getSaved());
        assertEquals(LocalDate.of(2027, 1, 1), saved.getDeadline());
        assertEquals("active", saved.getStatus());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());

        assertEquals(saved.getId(), response.getId());
        assertEquals("Laptop", response.getTitle());
        assertEquals("Tech", response.getCategory());
        assertEquals(new BigDecimal("1000"), response.getTarget());
        assertEquals(new BigDecimal("250"), response.getSaved());
        assertEquals(new BigDecimal("25.00"), response.getProgressPercent());
        assertEquals(new BigDecimal("750"), response.getRemaining());
        assertEquals(LocalDate.of(2027, 1, 1), response.getDeadline());
        assertEquals("active", response.getStatus());
    }

    @Test
    void createWithNullSavedDefaultsToZero() {
        GoalResponse response = goalService.create(userId, request("Trip", "500", null, null));

        assertEquals(BigDecimal.ZERO, response.getSaved());
        assertEquals(new BigDecimal("0.00"), response.getProgressPercent());
        assertEquals("active", response.getStatus());
    }

    @Test
    void createMarksGoalCompletedWhenSavedEqualsTarget() {
        GoalResponse response = goalService.create(userId, request("Trip", "500", "500", "paused"));

        assertEquals("completed", response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getProgressPercent());
        assertEquals(new BigDecimal("0"), response.getRemaining());
    }

    @Test
    void createKeepsPausedStatus() {
        assertEquals("paused", goalService.create(userId, request("Trip", "500", "10", " PAUSED ")).getStatus());
    }

    @Test
    void createNormalisesUppercaseActiveStatus() {
        assertEquals("active", goalService.create(userId, request("Trip", "500", "10", "ACTIVE")).getStatus());
    }

    @Test
    void createFallsBackToActiveForUnknownStatus() {
        assertEquals("active", goalService.create(userId, request("Trip", "500", "10", "completed")).getStatus());
        assertEquals("active", goalService.create(userId, request("Trip", "500", "10", "whatever")).getStatus());
        assertEquals("active", goalService.create(userId, request("Trip", "500", "10", "")).getStatus());
    }

    @Test
    void createRejectsNegativeSavedAmount() {
        GoalRequest request = request("Trip", "500", "-1", "active");

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("Saved amount cannot be negative", exception.getMessage());
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createRejectsSavedAboveTarget() {
        GoalRequest request = request("Trip", "500", "501", "active");

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("Saved cannot exceed target", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryException() {
        when(goalRepository.save(any(Goal.class))).thenThrow(new IllegalStateException("db down"));

        GoalRequest request = request("Trip", "500", "1", "active");
        assertThrows(IllegalStateException.class, () -> goalService.create(userId, request));
    }

    // ---------------------------------------------------------------- update

    @Test
    void updateChangesExistingGoal() {
        Goal existing = stored("Old", "100", "10");
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        GoalResponse response = goalService.update(userId, id, request("New", "200", "50", "paused"));

        assertEquals("New", existing.getTitle());
        assertEquals(new BigDecimal("200"), existing.getTarget());
        assertEquals("paused", existing.getStatus());
        assertEquals("New", response.getTitle());
        assertEquals(new BigDecimal("25.00"), response.getProgressPercent());
        verify(goalRepository).save(existing);
    }

    @Test
    void updateOfUnknownGoalThrowsNotFound() {
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        GoalRequest request = request("New", "200", "50", "active");
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> goalService.update(userId, id, request));

        assertEquals("Goal not found", exception.getMessage());
        verify(goalRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidAmountsBeforeLookup() {
        GoalRequest request = request("New", "200", "300", "active");

        assertThrows(ValidationException.class, () -> goalService.update(userId, id, request));
        verify(goalRepository, never()).findByIdAndUserIdAndDeletedFalse(any(), any());
    }

    // ---------------------------------------------------------------- list / response mapping

    @Test
    void listReturnsGoals() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(stored("A", "100", "40"), stored("B", "300", "0")));

        List<GoalResponse> rows = goalService.list(userId);

        assertEquals(2, rows.size());
        assertEquals("A", rows.get(0).getTitle());
        assertEquals(new BigDecimal("40.00"), rows.get(0).getProgressPercent());
        assertEquals(new BigDecimal("60"), rows.get(0).getRemaining());
        assertEquals(new BigDecimal("0.00"), rows.get(1).getProgressPercent());
    }

    @Test
    void listReturnsEmptyWhenUserHasNoGoals() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        assertTrue(goalService.list(userId).isEmpty());
    }

    @Test
    void listCapsProgressAtOneHundredWhenStoredSavedExceedsTarget() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(stored("Over", "100", "250")));

        GoalResponse response = goalService.list(userId).get(0);

        assertEquals(new BigDecimal("100.00"), response.getProgressPercent());
        assertEquals(BigDecimal.ZERO, response.getRemaining());
    }

    @Test
    void listHandlesStoredGoalWithNullAmounts() {
        Goal goal = stored("Broken", "100", "10");
        goal.setSaved(null);
        Goal noTarget = stored("NoTarget", "100", "10");
        noTarget.setTarget(null);
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal, noTarget));

        List<GoalResponse> rows = goalService.list(userId);

        assertEquals(BigDecimal.ZERO, rows.get(0).getSaved());
        assertEquals(new BigDecimal("0.00"), rows.get(0).getProgressPercent());
        assertEquals(new BigDecimal("100"), rows.get(0).getRemaining());
        assertEquals(BigDecimal.ZERO, rows.get(1).getTarget());
        assertEquals(BigDecimal.ZERO, rows.get(1).getProgressPercent());
        assertEquals(BigDecimal.ZERO, rows.get(1).getRemaining());
    }

    @Test
    void listPropagatesRepositoryException() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> goalService.list(userId));
    }

    // ---------------------------------------------------------------- delete

    @Test
    void softDeleteMarksGoalDeleted() {
        Goal existing = stored("A", "100", "10");
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        goalService.softDelete(userId, id);

        assertTrue(existing.isDeleted());
        assertNotNull(existing.getDeletedAt());
        assertNotNull(existing.getUpdatedAt());
        verify(goalRepository).save(existing);
    }

    @Test
    void softDeleteOfUnknownGoalThrowsNotFound() {
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.softDelete(userId, id));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryGoalDeleted() {
        Goal first = stored("A", "100", "10");
        Goal second = stored("B", "100", "10");
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));

        goalService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(goalRepository, times(2)).save(any(Goal.class));
    }

    @Test
    void softDeleteAllWithNoGoalsSavesNothing() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        goalService.softDeleteAll(userId);

        verify(goalRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private GoalRequest request(String title, String target, String saved, String status) {
        GoalRequest request = new GoalRequest();
        request.setTitle(title);
        request.setTarget(new BigDecimal(target));
        request.setSaved(saved == null ? null : new BigDecimal(saved));
        request.setStatus(status);
        return request;
    }

    private Goal stored(String title, String target, String saved) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setTarget(new BigDecimal(target));
        goal.setSaved(new BigDecimal(saved));
        goal.setStatus("active");
        return goal;
    }
}
