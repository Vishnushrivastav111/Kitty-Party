package com.microvault.finance.service;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;

import java.util.List;
import java.util.UUID;

public interface SavingsService {
    SavingsResponse create(UUID userId, SavingsRequest request);
    SavingsResponse update(UUID userId, UUID id, SavingsRequest request);
    List<SavingsResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
}
