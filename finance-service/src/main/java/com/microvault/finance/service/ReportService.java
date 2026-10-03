package com.microvault.finance.service;

import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;

import java.util.List;
import java.util.UUID;

public interface ReportService {
    ReportResponse generate(UUID userId, ReportRequest request);
    List<ReportResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
}
