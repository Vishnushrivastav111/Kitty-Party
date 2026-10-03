package com.microvault.finance.service;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;

import java.util.List;
import java.util.UUID;

public interface BudgetService {
    BudgetResponse create(UUID userId, BudgetRequest request);
    BudgetResponse update(UUID userId, UUID id, BudgetRequest request);
    List<BudgetResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
}
