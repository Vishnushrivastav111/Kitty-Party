package com.microvault.finance.repository;

import com.microvault.finance.entity.BudgetNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BudgetNotificationRepository extends JpaRepository<BudgetNotification, UUID> {
    boolean existsByUserIdAndReferenceKeyAndDeletedFalse(UUID userId, String referenceKey);
}
