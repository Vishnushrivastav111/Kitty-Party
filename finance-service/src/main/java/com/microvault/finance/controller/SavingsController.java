package com.microvault.finance.controller;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.service.RequestUser;
import com.microvault.finance.service.SavingsService;
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
@RequestMapping("/api/savings")
@Tag(name = "Savings", description = "Savings entries")
public class SavingsController {

    private final SavingsService savingsService;
    private final RequestUser requestUser;

    public SavingsController(SavingsService savingsService, RequestUser requestUser) {
        this.savingsService = savingsService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List savings")
    public List<SavingsResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return savingsService.list(requestUser.requireUser(authorization));
    }

    @PostMapping
    @Operation(summary = "Add a savings entry")
    public ResponseEntity<SavingsResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @Valid @RequestBody SavingsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.create(requestUser.requireUser(authorization), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings entry")
    public SavingsResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody SavingsRequest request) {
        return savingsService.update(requestUser.requireUser(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a savings entry")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        savingsService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every savings entry")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        savingsService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
