package com.microvault.finance.service;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse create(UUID userId, TransactionRequest request);
    TransactionResponse update(UUID userId, UUID id, TransactionRequest request);
    TransactionResponse get(UUID userId, UUID id);
    List<TransactionResponse> list(UUID userId);
    void softDelete(UUID userId, UUID id);
    void softDeleteAll(UUID userId);
    void softDeleteInternal(UUID userId, UUID id);
    /** Looks up a transaction for an approval request, including one that was deleted after approval. */
    TransactionResponse describeForApproval(UUID userId, UUID id);
}
