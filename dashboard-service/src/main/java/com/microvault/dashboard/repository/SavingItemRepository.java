package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.SavingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavingItemRepository extends JpaRepository<SavingItem, UUID> {

    List<SavingItem> findByUserIdAndDeletedFalseOrderByDateDesc(UUID userId);
}
