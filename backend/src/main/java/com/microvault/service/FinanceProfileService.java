package com.microvault.service;

import com.microvault.dto.FinanceProfileDTO;
import com.microvault.model.FinanceProfile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for financial profiles: validation, defaults and the
 * monthly surplus a member has left after expenses.
 */
public interface FinanceProfileService {

    FinanceProfileDTO createProfile(FinanceProfile financeProfile);

    FinanceProfileDTO getProfileById(UUID id);

    FinanceProfileDTO getProfileByUserId(UUID userId);

    List<FinanceProfileDTO> getAllProfiles();

    BigDecimal getMonthlySurplus(UUID userId);

    boolean updateProfile(FinanceProfile financeProfile);

    boolean softDeleteProfile(UUID id);
}
