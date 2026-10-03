package com.microvault.finance.controller;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.service.BudgetService;
import com.microvault.finance.service.RequestUser;
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
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Category budgets and usage")
public class BudgetController {

    private final BudgetService budgetService;
    private final RequestUser requestUser;

    public BudgetController(BudgetService budgetService, RequestUser requestUser) {
        this.budgetService = budgetService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List budgets", description = "Includes spent amount, remaining amount and a usage label.")
    public List<BudgetResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return budgetService.list(requestUser.requireUser(authorization));
    }

    @PostMapping
    @Operation(summary = "Create a budget")
    public ResponseEntity<BudgetResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.create(requestUser.requireUser(authorization), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget")
    public BudgetResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(requestUser.requireUser(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a budget")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        budgetService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every budget")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        budgetService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
