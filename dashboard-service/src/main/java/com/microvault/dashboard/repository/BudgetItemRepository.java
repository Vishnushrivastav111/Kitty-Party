package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, UUID> {

    List<BudgetItem> findByUserIdAndDeletedFalseOrderByCategoryAsc(UUID userId);
}
