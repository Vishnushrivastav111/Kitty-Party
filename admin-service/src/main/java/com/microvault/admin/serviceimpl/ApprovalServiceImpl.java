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
import com.microvault.admin.service.ApprovalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ApprovalServiceImpl implements ApprovalService {
    private final ApprovalRequestRepository repository;
    private final AuthDirectory authDirectory;
    private final FinanceDirectory financeDirectory;

    public ApprovalServiceImpl(ApprovalRequestRepository repository,
                               AuthDirectory authDirectory,
                               FinanceDirectory financeDirectory) {
        this.repository = repository;
        this.authDirectory = authDirectory;
        this.financeDirectory = financeDirectory;
    }

    @Override
    public ApprovalRequestResponse create(UUID requesterId, ApprovalRequestCreate request) {
        String type = request.getRequestType().trim().toLowerCase();
        if (!"transaction-delete".equals(type)) {
            throw new ValidationException("Unsupported approval request type");
        }
        if (repository.existsByRequesterIdAndResourceIdAndRequestTypeAndStatusAndDeletedFalse(
                requesterId, request.getResourceId(), type, "pending")) {
            throw new ValidationException("An approval request for this transaction is already pending");
        }

        ApprovalRequest entity = new ApprovalRequest();
        entity.setId(UUID.randomUUID());
        entity.setRequesterId(requesterId);
        entity.setRequestType(type);
        entity.setResourceId(request.getResourceId());
        entity.setReason(request.getReason() == null ? null : request.getReason().trim());
        entity.setStatus("pending");
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(false);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> mine(UUID requesterId) {
        Map<UUID, UserCard> users = users();
        return repository.findByRequesterIdAndDeletedFalseOrderByCreatedAtDesc(requesterId)
                .stream().map(entity -> toResponse(entity, users)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> all() {
        Map<UUID, UserCard> users = users();
        return repository.findByDeletedFalseOrderByCreatedAtDesc()
                .stream().map(entity -> toResponse(entity, users)).toList();
    }

    @Override
    public ApprovalRequestResponse approve(UUID reviewerId, UUID id) {
        ApprovalRequest entity = loadPending(id);
        if (!"transaction-delete".equals(entity.getRequestType())) {
            throw new ValidationException("Unsupported approval request type");
        }
        financeDirectory.softDeleteTransaction(entity.getRequesterId(), entity.getResourceId());
        LocalDateTime now = LocalDateTime.now();
        entity.setStatus("approved");
        entity.setReviewerId(reviewerId);
        entity.setReviewedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(repository.save(entity));
    }

    @Override
    public ApprovalRequestResponse reject(UUID reviewerId, UUID id, String reason) {
        ApprovalRequest entity = loadPending(id);
        LocalDateTime now = LocalDateTime.now();
        entity.setStatus("rejected");
        entity.setReviewerId(reviewerId);
        entity.setReviewedAt(now);
        entity.setReason(reason == null || reason.isBlank() ? entity.getReason() : reason.trim());
        entity.setUpdatedAt(now);
        return toResponse(repository.save(entity));
    }

    private ApprovalRequest loadPending(UUID id) {
        ApprovalRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        if (!"pending".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("Approval request has already been reviewed");
        }
        return entity;
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest entity) {
        return toResponse(entity, users());
    }

    private Map<UUID, UserCard> users() {
        try {
            Map<UUID, UserCard> byId = new HashMap<>();
            for (UserCard user : authDirectory.listUsers(null)) {
                if (user != null && user.getId() != null) {
                    byId.put(user.getId(), user);
                }
            }
            return byId;
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private String describe(ApprovalRequest entity) {
        FinanceDirectory.TransactionSummary transaction = financeDirectory.transaction(entity.getRequesterId(), entity.getResourceId());
        if (transaction == null) {
            return "Transaction (details not available)";
        }
        StringBuilder label = new StringBuilder(transaction.getName() == null ? "Transaction" : transaction.getName());
        if (transaction.getCategory() != null && !transaction.getCategory().isBlank()) {
            label.append(" · ").append(transaction.getCategory());
        }
        if (transaction.getAmount() != null) {
            label.append(" · ₹").append(transaction.getAmount().stripTrailingZeros().toPlainString());
        }
        if (transaction.getDate() != null) {
            label.append(" · ").append(transaction.getDate());
        }
        return label.toString();
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest entity, Map<UUID, UserCard> users) {
        ApprovalRequestResponse response = new ApprovalRequestResponse();
        response.setId(entity.getId());
        response.setRequesterId(entity.getRequesterId());
        UserCard requester = users.get(entity.getRequesterId());
        if (requester != null) {
            response.setRequesterName(requester.getFullName());
            response.setRequesterEmail(requester.getEmail());
        }
        UserCard reviewer = entity.getReviewerId() == null ? null : users.get(entity.getReviewerId());
        if (reviewer != null) {
            response.setReviewerName(reviewer.getFullName());
        }
        response.setRequestType(entity.getRequestType());
        response.setResourceId(entity.getResourceId());
        response.setResourceLabel(describe(entity));
        response.setReason(entity.getReason());
        response.setStatus(entity.getStatus());
        response.setReviewerId(entity.getReviewerId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setReviewedAt(entity.getReviewedAt());
        return response;
    }
}
