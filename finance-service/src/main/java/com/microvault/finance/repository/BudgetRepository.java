package com.microvault.finance.repository;

import com.microvault.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserIdAndDeletedFalseOrderByCategoryAsc(UUID userId);
    Optional<Budget> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
    Optional<Budget> findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(UUID userId, String category);
}
