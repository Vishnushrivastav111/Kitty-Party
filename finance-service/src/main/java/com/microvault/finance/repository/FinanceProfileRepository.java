package com.microvault.finance.repository;

import com.microvault.finance.entity.FinanceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceProfileRepository extends JpaRepository<FinanceProfile, UUID> {
    Optional<FinanceProfile> findByUserIdAndDeletedFalse(UUID userId);
    Optional<FinanceProfile> findByIdAndDeletedFalse(UUID id);
    List<FinanceProfile> findByDeletedFalse();
}
