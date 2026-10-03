package com.microvault.finance.repository;

import com.microvault.finance.entity.SavingsEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsRepository extends JpaRepository<SavingsEntry, UUID> {
    List<SavingsEntry> findByUserIdAndDeletedFalseOrderByDateDesc(UUID userId);
    Optional<SavingsEntry> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
