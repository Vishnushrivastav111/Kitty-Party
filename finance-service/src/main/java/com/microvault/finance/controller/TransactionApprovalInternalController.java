package com.microvault.finance.controller;

import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.GetMapping;
import com.microvault.finance.service.TransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/transactions")
public class TransactionApprovalInternalController {
    private final TransactionService transactionService;
    private final String internalToken;

    public TransactionApprovalInternalController(TransactionService transactionService,
                                                 @Value("${microvault.internal.token}") String internalToken) {
        this.transactionService = transactionService;
        this.internalToken = internalToken;
    }

    @GetMapping("/{id}")
    public TransactionResponse describe(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        if (token == null || !token.equals(internalToken)) throw new UnauthorizedException("Internal access is required");
        return transactionService.describeForApproval(userId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAfterApproval(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        if (token == null || !token.equals(internalToken)) throw new UnauthorizedException("Internal access is required");
        transactionService.softDeleteInternal(userId, id);
        return ResponseEntity.noContent().build();
    }
}
