package com.microvault.finance.service;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;

import java.util.List;
import java.util.UUID;

public interface AffordabilityService {
    AffordabilityResponse check(UUID userId, AffordabilityRequest request);
    List<AffordabilityResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
}
