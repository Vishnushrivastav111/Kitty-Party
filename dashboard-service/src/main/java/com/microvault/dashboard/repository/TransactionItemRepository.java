package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

    List<TransactionItem> findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(UUID userId);
}
