package com.microvault.dao;

import com.microvault.model.FinanceProfile;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "finance_profiles" table. Only data access is
 * described here, no business rules.
 */
public interface FinanceProfileDAO {

    FinanceProfile create(FinanceProfile financeProfile);

    FinanceProfile findById(UUID id);

    FinanceProfile findByUserId(UUID userId);

    List<FinanceProfile> findAll();

    boolean update(FinanceProfile financeProfile);

    boolean softDelete(UUID id);
}
