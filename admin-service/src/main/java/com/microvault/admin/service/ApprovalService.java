package com.microvault.admin.service;

import com.microvault.admin.dto.ApprovalRequestCreate;
import com.microvault.admin.dto.ApprovalRequestResponse;

import java.util.List;
import java.util.UUID;

public interface ApprovalService {
    ApprovalRequestResponse create(UUID requesterId, ApprovalRequestCreate request);
    List<ApprovalRequestResponse> mine(UUID requesterId);
    List<ApprovalRequestResponse> all();
    ApprovalRequestResponse approve(UUID reviewerId, UUID id);
    ApprovalRequestResponse reject(UUID reviewerId, UUID id, String reason);
}
