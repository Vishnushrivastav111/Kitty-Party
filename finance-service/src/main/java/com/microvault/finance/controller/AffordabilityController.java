package com.microvault.finance.controller;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.service.AffordabilityService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/affordability")
@Tag(name = "Affordability", description = "Checks whether a purchase fits the member's available money")
public class AffordabilityController {

    private final AffordabilityService affordabilityService;
    private final RequestUser requestUser;

    public AffordabilityController(AffordabilityService affordabilityService, RequestUser requestUser) {
        this.affordabilityService = affordabilityService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List affordability checks")
    public List<AffordabilityResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return affordabilityService.list(requestUser.requireUser(authorization));
    }

    @PostMapping({"/check", ""})
    @Operation(summary = "Check affordability",
            description = "Available capacity is monthly surplus plus 30% of savings. The response includes a verdict and a short plan.")
    public ResponseEntity<AffordabilityResponse> check(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @Valid @RequestBody AffordabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(affordabilityService.check(requestUser.requireUser(authorization), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an affordability check")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        affordabilityService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every affordability check")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        affordabilityService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
