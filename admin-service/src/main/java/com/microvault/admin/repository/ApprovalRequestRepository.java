package com.microvault.admin.repository;

import com.microvault.admin.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    List<ApprovalRequest> findByDeletedFalseOrderByCreatedAtDesc();
    List<ApprovalRequest> findByRequesterIdAndDeletedFalseOrderByCreatedAtDesc(UUID requesterId);
    Optional<ApprovalRequest> findByIdAndDeletedFalse(UUID id);
    boolean existsByRequesterIdAndResourceIdAndRequestTypeAndStatusAndDeletedFalse(
            UUID requesterId, UUID resourceId, String requestType, String status);
}
