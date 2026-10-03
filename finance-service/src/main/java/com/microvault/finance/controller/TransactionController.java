package com.microvault.finance.controller;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.service.RequestUser;
import com.microvault.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Income and expense transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final RequestUser requestUser;

    public TransactionController(TransactionService transactionService, RequestUser requestUser) {
        this.transactionService = transactionService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List transactions")
    public List<TransactionResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return transactionService.list(requestUser.requireUser(authorization));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction")
    public TransactionResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable UUID id) {
        return transactionService.get(requestUser.requireUser(authorization), id);
    }

    @PostMapping
    @Operation(summary = "Create a transaction", description = "Creates an income or expense transaction for the logged-in user.")
    public ResponseEntity<TransactionResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @Valid @RequestBody TransactionRequest request) {
        TransactionResponse saved = transactionService.create(requestUser.requireUser(authorization), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction")
    public TransactionResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(requestUser.requireUser(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a transaction")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        transactionService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every transaction for the user")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        transactionService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
