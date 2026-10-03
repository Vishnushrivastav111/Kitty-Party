package com.microvault.finance.repository;

import com.microvault.finance.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
    Optional<Goal> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
