package com.microvault.financeprofile.repository;

import com.microvault.financeprofile.model.FinanceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database access for finance_profiles. Reads skip soft-deleted rows.
 */
public interface FinanceProfileRepository extends JpaRepository<FinanceProfile, UUID> {

    Optional<FinanceProfile> findByIdAndIsDeletedFalse(UUID id);

    Optional<FinanceProfile> findByUserIdAndIsDeletedFalse(UUID userId);

    List<FinanceProfile> findByIsDeletedFalseOrderByCreatedAtDesc();
}
