package com.microvault.admin.serviceimpl;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.client.FinanceDirectory;
import com.microvault.admin.dto.ApprovalRequestCreate;
import com.microvault.admin.dto.ApprovalRequestResponse;
import com.microvault.admin.dto.UserCard;
import com.microvault.admin.entity.ApprovalRequest;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.repository.ApprovalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {

    private static final UUID REQUESTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REVIEWER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID RESOURCE = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID REQUEST_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private ApprovalRequestRepository repository;
    @Mock
    private AuthDirectory authDirectory;
    @Mock
    private FinanceDirectory financeDirectory;

    private ApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApprovalServiceImpl(repository, authDirectory, financeDirectory);
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aDeletionRequestIsStoredAsPending() {
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(REQUESTER, "Ada Lovelace", "ada@example.com")));
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary("Rent", "Housing", "1500.00", "2026-10-01"));

        ApprovalRequestResponse response = service.create(REQUESTER, request(" Transaction-Delete ", "  paid twice  "));

        ArgumentCaptor<ApprovalRequest> saved = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(repository).save(saved.capture());
        ApprovalRequest entity = saved.getValue();
        assertEquals(REQUESTER, entity.getRequesterId());
        assertEquals("transaction-delete", entity.getRequestType());
        assertEquals(RESOURCE, entity.getResourceId());
        assertEquals("paid twice", entity.getReason());
        assertEquals("pending", entity.getStatus());
        assertFalse(entity.isDeleted());
        assertEquals("Ada Lovelace", response.getRequesterName());
        assertEquals("ada@example.com", response.getRequesterEmail());
        assertEquals("Rent · Housing · ₹1500 · 2026-10-01", response.getResourceLabel());
        assertNull(response.getReviewerName());
    }

    @Test
    void anUnsupportedRequestTypeIsRejected() {
        ApprovalRequestCreate create = request("budget-delete", "no");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.create(REQUESTER, create));

        assertEquals("Unsupported approval request type", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void aSecondPendingRequestForTheSameTransactionIsRejected() {
        when(repository.existsByRequesterIdAndResourceIdAndRequestTypeAndStatusAndDeletedFalse(
                REQUESTER, RESOURCE, "transaction-delete", "pending")).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.create(REQUESTER, request("transaction-delete", null)));

        assertEquals("An approval request for this transaction is already pending", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void aDirectoryFailureStillStoresTheRequestWithoutAName() {
        when(authDirectory.listUsers(null)).thenThrow(new IllegalStateException("auth down"));
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(null);

        ApprovalRequestResponse response = service.create(REQUESTER, request("transaction-delete", null));

        assertNull(response.getRequesterName());
        assertNull(response.getReason());
        assertEquals("Transaction (details not available)", response.getResourceLabel());
    }

    @Test
    void incompleteDirectoryRowsAreIgnored() {
        UserCard nameless = new UserCard();
        when(authDirectory.listUsers(null)).thenReturn(java.util.Arrays.asList(null, nameless));
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary(null, null, null, null));

        ApprovalRequestResponse response = service.create(REQUESTER, request("transaction-delete", "  "));

        assertNull(response.getRequesterName());
        assertEquals("Transaction", response.getResourceLabel());
        assertEquals("", response.getReason());
    }

    @Test
    void mineAndAllAttachRequesterAndReviewerNames() {
        ApprovalRequest pending = stored("pending", REVIEWER, "please");
        when(repository.findByRequesterIdAndDeletedFalseOrderByCreatedAtDesc(REQUESTER)).thenReturn(List.of(pending));
        when(repository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(pending));
        when(authDirectory.listUsers(null)).thenReturn(List.of(
                card(REQUESTER, "Ada Lovelace", "ada@example.com"),
                card(REVIEWER, "Grace Hopper", "grace@example.com")));
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary("Coffee", " ", "10.50", null));

        ApprovalRequestResponse mine = service.mine(REQUESTER).get(0);
        ApprovalRequestResponse all = service.all().get(0);

        assertEquals("Ada Lovelace", mine.getRequesterName());
        assertEquals("Grace Hopper", mine.getReviewerName());
        assertEquals("Coffee · ₹10.5", mine.getResourceLabel());
        assertEquals(mine.getRequesterEmail(), all.getRequesterEmail());
        assertEquals(REQUEST_ID, all.getId());
    }

    @Test
    void approvingDeletesTheTransactionAndRecordsTheReviewer() {
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(stored("PENDING", null, "please")));
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(REVIEWER, "Grace Hopper", "grace@example.com")));
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary("Rent", "", null, "2026-10-02"));

        ApprovalRequestResponse response = service.approve(REVIEWER, REQUEST_ID);

        verify(financeDirectory).softDeleteTransaction(REQUESTER, RESOURCE);
        assertEquals("approved", response.getStatus());
        assertEquals(REVIEWER, response.getReviewerId());
        assertEquals("Grace Hopper", response.getReviewerName());
        assertEquals("Rent · 2026-10-02", response.getResourceLabel());
        assertTrue(response.getReviewedAt() != null);
    }

    @Test
    void approvingAnUnknownOrClosedOrUnsupportedRequestFails() {
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.empty());
        assertEquals("Approval request not found",
                assertThrows(ResourceNotFoundException.class, () -> service.approve(REVIEWER, REQUEST_ID)).getMessage());

        ApprovalRequest closed = stored("approved", REVIEWER, "done");
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(closed));
        assertEquals("Approval request has already been reviewed",
                assertThrows(ValidationException.class, () -> service.approve(REVIEWER, REQUEST_ID)).getMessage());

        ApprovalRequest other = stored("pending", null, "done");
        other.setRequestType("budget-delete");
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(other));
        assertEquals("Unsupported approval request type",
                assertThrows(ValidationException.class, () -> service.approve(REVIEWER, REQUEST_ID)).getMessage());
        verify(financeDirectory, never()).softDeleteTransaction(any(), any());
    }

    @Test
    void rejectingReplacesASuppliedReasonAndKeepsABlankOne() {
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(stored("pending", null, "original")));
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary("Tea", null, "0.00", null));

        ApprovalRequestResponse replaced = service.reject(REVIEWER, REQUEST_ID, "  not this one  ");
        assertEquals("not this one", replaced.getReason());
        assertEquals("rejected", replaced.getStatus());
        assertEquals("Tea · ₹0", replaced.getResourceLabel());

        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(stored("pending", null, "original")));
        assertEquals("original", service.reject(REVIEWER, REQUEST_ID, "   ").getReason());

        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(stored("pending", null, "original")));
        assertEquals("original", service.reject(REVIEWER, REQUEST_ID, null).getReason());
    }

    @Test
    void rejectingAMissingOrClosedRequestFails() {
        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.reject(REVIEWER, REQUEST_ID, "no"));

        when(repository.findByIdAndDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(stored("REJECTED", REVIEWER, "no")));
        assertThrows(ValidationException.class, () -> service.reject(REVIEWER, REQUEST_ID, "again"));
    }

    @Test
    void approvalModelsRoundTripEveryField() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 1, 8, 0);
        LocalDateTime reviewed = created.plusHours(2);
        ApprovalRequest entity = new ApprovalRequest();
        entity.setId(REQUEST_ID);
        entity.setRequesterId(REQUESTER);
        entity.setRequestType("transaction-delete");
        entity.setResourceId(RESOURCE);
        entity.setStatus("pending");
        entity.setReason("please");
        entity.setReviewerId(REVIEWER);
        entity.setReviewedAt(reviewed);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(reviewed);
        entity.setDeleted(true);
        entity.setDeletedAt(reviewed);

        assertEquals(REQUEST_ID, entity.getId());
        assertEquals(REQUESTER, entity.getRequesterId());
        assertEquals("transaction-delete", entity.getRequestType());
        assertEquals(RESOURCE, entity.getResourceId());
        assertEquals("pending", entity.getStatus());
        assertEquals("please", entity.getReason());
        assertEquals(REVIEWER, entity.getReviewerId());
        assertEquals(reviewed, entity.getReviewedAt());
        assertEquals(created, entity.getCreatedAt());
        assertEquals(reviewed, entity.getUpdatedAt());
        assertTrue(entity.isDeleted());
        assertEquals(reviewed, entity.getDeletedAt());

        ApprovalRequestCreate create = new ApprovalRequestCreate();
        create.setRequestType("transaction-delete");
        create.setResourceId(RESOURCE);
        create.setReason("please");
        assertEquals("transaction-delete", create.getRequestType());
        assertEquals(RESOURCE, create.getResourceId());
        assertEquals("please", create.getReason());

        ApprovalRequestResponse response = new ApprovalRequestResponse();
        response.setId(REQUEST_ID);
        response.setRequesterId(REQUESTER);
        response.setRequesterName("Ada");
        response.setRequesterEmail("ada@example.com");
        response.setRequestType("transaction-delete");
        response.setResourceId(RESOURCE);
        response.setResourceLabel("Rent");
        response.setReason("please");
        response.setStatus("pending");
        response.setReviewerId(REVIEWER);
        response.setReviewerName("Grace");
        response.setCreatedAt(created);
        response.setReviewedAt(reviewed);
        assertEquals(REQUEST_ID, response.getId());
        assertEquals(REQUESTER, response.getRequesterId());
        assertEquals("Ada", response.getRequesterName());
        assertEquals("ada@example.com", response.getRequesterEmail());
        assertEquals("transaction-delete", response.getRequestType());
        assertEquals(RESOURCE, response.getResourceId());
        assertEquals("Rent", response.getResourceLabel());
        assertEquals("please", response.getReason());
        assertEquals("pending", response.getStatus());
        assertEquals(REVIEWER, response.getReviewerId());
        assertEquals("Grace", response.getReviewerName());
        assertEquals(created, response.getCreatedAt());
        assertEquals(reviewed, response.getReviewedAt());
    }

    @ParameterizedTest(name = "{0} / {1} / {2} / {3}")
    @MethodSource("labels")
    void everyTransactionShapeBuildsTheSameLabel(String name, String category, String amount, String date) {
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        FinanceDirectory.TransactionSummary summary = new FinanceDirectory.TransactionSummary();
        summary.setName(name);
        summary.setCategory(category);
        summary.setAmount(amount == null ? null : new BigDecimal(amount));
        summary.setDate(date);
        when(financeDirectory.transaction(REQUESTER, RESOURCE)).thenReturn(summary);

        ApprovalRequestResponse response = service.create(REQUESTER, request("transaction-delete", "check"));

        assertEquals(expectedLabel(name, category, amount, date), response.getResourceLabel());
        assertEquals("pending", response.getStatus());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> labels() {
        String[] names = {null, "Rent", "Coffee", "Salary", "Tea", "Bus"};
        String[] categories = {null, "", " ", "Housing", "Food", "Travel"};
        String[] amounts = {null, "0", "10.50", "100.00", "2499"};
        String[] dates = {null, "2026-10-01", "2026-01-31"};
        return java.util.Arrays.stream(names).flatMap(name -> java.util.Arrays.stream(categories)
                .flatMap(category -> java.util.Arrays.stream(amounts)
                        .flatMap(amount -> java.util.Arrays.stream(dates)
                                .map(date -> org.junit.jupiter.params.provider.Arguments.of(name, category, amount, date)))));
    }

    private static String expectedLabel(String name, String category, String amount, String date) {
        StringBuilder label = new StringBuilder(name == null ? "Transaction" : name);
        if (category != null && !category.isBlank()) {
            label.append(" · ").append(category);
        }
        if (amount != null) {
            label.append(" · ₹").append(new BigDecimal(amount).stripTrailingZeros().toPlainString());
        }
        if (date != null) {
            label.append(" · ").append(date);
        }
        return label.toString();
    }

    private ApprovalRequestCreate request(String type, String reason) {
        ApprovalRequestCreate create = new ApprovalRequestCreate();
        create.setRequestType(type);
        create.setResourceId(RESOURCE);
        create.setReason(reason);
        return create;
    }

    private ApprovalRequest stored(String status, UUID reviewer, String reason) {
        ApprovalRequest entity = new ApprovalRequest();
        entity.setId(REQUEST_ID);
        entity.setRequesterId(REQUESTER);
        entity.setRequestType("transaction-delete");
        entity.setResourceId(RESOURCE);
        entity.setStatus(status);
        entity.setReason(reason);
        entity.setReviewerId(reviewer);
        entity.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
        return entity;
    }

    private UserCard card(UUID id, String name, String email) {
        UserCard card = new UserCard();
        card.setId(id);
        card.setFullName(name);
        card.setEmail(email);
        return card;
    }

    private FinanceDirectory.TransactionSummary summary(String name, String category, String amount, String date) {
        FinanceDirectory.TransactionSummary summary = new FinanceDirectory.TransactionSummary();
        summary.setName(name);
        summary.setCategory(category);
        summary.setAmount(amount == null ? null : new BigDecimal(amount));
        summary.setDate(date);
        return summary;
    }
}
