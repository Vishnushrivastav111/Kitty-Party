package com.microvault.finance.controller;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.service.GoalService;
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
@RequestMapping("/api/goals")
@Tag(name = "Goals", description = "Financial goals and progress")
public class GoalController {

    private final GoalService goalService;
    private final RequestUser requestUser;

    public GoalController(GoalService goalService, RequestUser requestUser) {
        this.goalService = goalService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List goals")
    public List<GoalResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return goalService.list(requestUser.requireUser(authorization));
    }

    @PostMapping
    @Operation(summary = "Create a goal")
    public ResponseEntity<GoalResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(requestUser.requireUser(authorization), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a goal")
    public GoalResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable UUID id,
                               @Valid @RequestBody GoalRequest request) {
        return goalService.update(requestUser.requireUser(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a goal")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        goalService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every goal")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        goalService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
