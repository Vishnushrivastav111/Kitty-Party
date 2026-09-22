package com.microvault.service;

import com.microvault.dto.AffordabilityCheckDTO;
import com.microvault.model.AffordabilityCheck;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for affordability checks: validation and the verdict
 * that tells a member whether a planned purchase fits their money.
 */
public interface AffordabilityCheckService {

    AffordabilityCheckDTO checkAffordability(UUID userId, String itemName, BigDecimal amount, String priority,
                                             LocalDate checkDate, BigDecimal monthlyIncome,
                                             BigDecimal monthlyExpenses, BigDecimal totalSavings);

    AffordabilityCheckDTO createCheck(AffordabilityCheck affordabilityCheck);

    AffordabilityCheckDTO getCheckById(UUID id);

    List<AffordabilityCheckDTO> getAllChecks();

    List<AffordabilityCheckDTO> getChecksByUserId(UUID userId);

    boolean updateCheck(AffordabilityCheck affordabilityCheck);

    boolean softDeleteCheck(UUID id);
}
