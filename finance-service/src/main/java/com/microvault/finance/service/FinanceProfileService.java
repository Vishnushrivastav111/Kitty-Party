package com.microvault.finance.service;

import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;

import java.util.List;
import java.util.UUID;

public interface FinanceProfileService {
    FinanceProfileResponse saveForUser(UUID userId, FinanceProfileRequest request);
    FinanceProfileResponse getByUserId(UUID userId);
    MonthlySummaryResponse summary(UUID userId);
    void skipSetup(UUID userId);
    List<UUID> activeUserIds();
    FinanceWorkspaceResponse workspace(UUID userId);
}
