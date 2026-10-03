package com.microvault.finance.repository;

import com.microvault.finance.entity.AffordabilityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffordabilityRepository extends JpaRepository<AffordabilityCheck, UUID> {
    List<AffordabilityCheck> findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(UUID userId);
    Optional<AffordabilityCheck> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
