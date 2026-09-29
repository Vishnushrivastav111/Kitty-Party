package com.microvault.financeprofile.service;

import com.microvault.financeprofile.dto.FinanceProfileRequest;
import com.microvault.financeprofile.dto.FinanceProfileResponse;
import com.microvault.financeprofile.dto.MonthlySummaryResponse;
import com.microvault.financeprofile.dto.SurplusResponse;

import java.util.List;
import java.util.UUID;

/**
 * Create, read, update and soft-delete a member's financial profile,
 * plus the monthly surplus used by the dashboard.
 */
public interface FinanceProfileService {

    FinanceProfileResponse createProfile(FinanceProfileRequest request);

    FinanceProfileResponse getProfileById(UUID id);

    FinanceProfileResponse getProfileByUserId(UUID userId);

    List<FinanceProfileResponse> getAllProfiles();

    SurplusResponse getMonthlySurplus(UUID userId);

    MonthlySummaryResponse getMonthlySummary(UUID userId);

    FinanceProfileResponse updateProfile(UUID id, FinanceProfileRequest request);

    void softDeleteProfile(UUID id);
}
