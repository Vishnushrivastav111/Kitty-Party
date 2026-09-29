package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.FinanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FinanceSnapshotRepository extends JpaRepository<FinanceSnapshot, UUID> {

    Optional<FinanceSnapshot> findByUserIdAndDeletedFalse(UUID userId);
}
