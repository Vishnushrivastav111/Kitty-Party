package com.microvault.dao;

import com.microvault.model.AffordabilityCheck;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "affordability_checks" table. Only data access
 * is described here, no business rules.
 */
public interface AffordabilityCheckDAO {

    AffordabilityCheck create(AffordabilityCheck affordabilityCheck);

    AffordabilityCheck findById(UUID id);

    List<AffordabilityCheck> findAll();

    List<AffordabilityCheck> findByUserId(UUID userId);

    boolean update(AffordabilityCheck affordabilityCheck);

    boolean softDelete(UUID id);
}
