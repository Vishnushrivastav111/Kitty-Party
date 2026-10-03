package com.microvault.finance.service;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;

import java.util.List;
import java.util.UUID;

public interface GoalService {
    GoalResponse create(UUID userId, GoalRequest request);
    GoalResponse update(UUID userId, UUID id, GoalRequest request);
    List<GoalResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
}
