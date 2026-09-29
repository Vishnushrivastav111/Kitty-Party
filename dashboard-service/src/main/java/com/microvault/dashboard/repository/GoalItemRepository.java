package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.GoalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalItemRepository extends JpaRepository<GoalItem, UUID> {

    List<GoalItem> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
