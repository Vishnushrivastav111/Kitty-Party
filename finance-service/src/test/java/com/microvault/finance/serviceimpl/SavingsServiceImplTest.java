package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.SavingsRepository;
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
class SavingsServiceImplTest {

    @Mock
    private SavingsRepository savingsRepository;

    @InjectMocks
    private SavingsServiceImpl savingsService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(savingsRepository.save(any(SavingsEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    void createPersistsEntry() {
        SavingsRequest request = request("  FD  ", "5000", LocalDate.now());
        request.setCategory("Bank");
        request.setNote("fixed deposit");

        SavingsResponse response = savingsService.create(userId, request);

        ArgumentCaptor<SavingsEntry> captor = ArgumentCaptor.forClass(SavingsEntry.class);
        verify(savingsRepository).save(captor.capture());
        SavingsEntry saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("FD", saved.getTitle());
        assertEquals("Bank", saved.getCategory());
        assertEquals(new BigDecimal("5000"), saved.getAmount());
        assertEquals(LocalDate.now(), saved.getDate());
        assertEquals("fixed deposit", saved.getNote());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());

        assertEquals(saved.getId(), response.getId());
        assertEquals("FD", response.getTitle());
        assertEquals("Bank", response.getCategory());
        assertEquals(new BigDecimal("5000"), response.getAmount());
        assertEquals(LocalDate.now(), response.getDate());
        assertEquals("fixed deposit", response.getNote());
    }

    @Test
    void createRejectsFutureDate() {
        SavingsRequest request = request("FD", "5000", LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class, () -> savingsService.create(userId, request));

        assertEquals("Date cannot be in the future", exception.getMessage());
        verify(savingsRepository, never()).save(any());
    }

    @Test
    void createPropagatesRepositoryException() {
        when(savingsRepository.save(any(SavingsEntry.class))).thenThrow(new IllegalStateException("db down"));

        SavingsRequest request = request("FD", "5000", LocalDate.now());
        assertThrows(IllegalStateException.class, () -> savingsService.create(userId, request));
    }

    // ---------------------------------------------------------------- update

    @Test
    void updateChangesExistingEntry() {
        SavingsEntry existing = stored("Old");
        when(savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        SavingsResponse response = savingsService.update(userId, id, request("New", "700", LocalDate.now()));

        assertEquals("New", existing.getTitle());
        assertEquals(new BigDecimal("700"), existing.getAmount());
        assertEquals("New", response.getTitle());
        assertNotNull(existing.getUpdatedAt());
        verify(savingsRepository).save(existing);
    }

    @Test
    void updateOfUnknownEntryThrowsNotFound() {
        when(savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        SavingsRequest request = request("New", "700", LocalDate.now());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> savingsService.update(userId, id, request));

        assertEquals("Savings entry not found", exception.getMessage());
        verify(savingsRepository, never()).save(any());
    }

    @Test
    void updateRejectsFutureDateBeforeLookup() {
        SavingsRequest request = request("New", "700", LocalDate.now().plusDays(2));

        assertThrows(ValidationException.class, () -> savingsService.update(userId, id, request));
        verify(savingsRepository, never()).findByIdAndUserIdAndDeletedFalse(any(), any());
    }

    // ---------------------------------------------------------------- list

    @Test
    void listReturnsEntries() {
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId))
                .thenReturn(List.of(stored("A"), stored("B")));

        List<SavingsResponse> rows = savingsService.list(userId);

        assertEquals(2, rows.size());
        assertEquals("A", rows.get(0).getTitle());
        assertEquals("B", rows.get(1).getTitle());
    }

    @Test
    void listReturnsEmptyWhenNothingSaved() {
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of());

        assertTrue(savingsService.list(userId).isEmpty());
    }

    @Test
    void listPropagatesRepositoryException() {
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> savingsService.list(userId));
    }

    // ---------------------------------------------------------------- delete

    @Test
    void softDeleteMarksEntryDeleted() {
        SavingsEntry existing = stored("A");
        when(savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));

        savingsService.softDelete(userId, id);

        assertTrue(existing.isDeleted());
        assertNotNull(existing.getDeletedAt());
        assertNotNull(existing.getUpdatedAt());
        verify(savingsRepository).save(existing);
    }

    @Test
    void softDeleteOfUnknownEntryThrowsNotFound() {
        when(savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savingsService.softDelete(userId, id));
        verify(savingsRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryEntryDeleted() {
        SavingsEntry first = stored("A");
        SavingsEntry second = stored("B");
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(first, second));

        savingsService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(savingsRepository, times(2)).save(any(SavingsEntry.class));
    }

    @Test
    void softDeleteAllWithNoEntriesSavesNothing() {
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of());

        savingsService.softDeleteAll(userId);

        verify(savingsRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private SavingsRequest request(String title, String amount, LocalDate date) {
        SavingsRequest request = new SavingsRequest();
        request.setTitle(title);
        request.setAmount(new BigDecimal(amount));
        request.setDate(date);
        return request;
    }

    private SavingsEntry stored(String title) {
        SavingsEntry entry = new SavingsEntry();
        entry.setId(id);
        entry.setUserId(userId);
        entry.setTitle(title);
        entry.setAmount(new BigDecimal("100"));
        entry.setDate(LocalDate.of(2026, 5, 1));
        return entry;
    }
}
