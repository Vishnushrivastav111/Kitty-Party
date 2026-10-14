package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.ApprovalRequestCreate;
import com.microvault.admin.dto.ApprovalRequestResponse;
import com.microvault.admin.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/approvals")
@Tag(name = "Approval Requests")
public class ApprovalController {
    private final ApprovalService approvalService;
    private final AuthDirectory authDirectory;

    public ApprovalController(ApprovalService approvalService, AuthDirectory authDirectory) {
        this.approvalService = approvalService;
        this.authDirectory = authDirectory;
    }

    @PostMapping
    @Operation(summary = "Submit an approval request from a user")
    public ResponseEntity<ApprovalRequestResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ApprovalRequestCreate request) {
        AuthDirectory.SessionUser user = authDirectory.requireSession(authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(approvalService.create(user.getUserId(), request));
    }

    @GetMapping("/mine")
    @Operation(summary = "List approval requests submitted by the logged-in user")
    public List<ApprovalRequestResponse> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthDirectory.SessionUser user = authDirectory.requireSession(authorization);
        return approvalService.mine(user.getUserId());
    }

    @GetMapping
    @Operation(summary = "List all approval requests for admins")
    public List<ApprovalRequestResponse> all(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authDirectory.requireAdmin(authorization);
        return approvalService.all();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an approval request")
    public ApprovalRequestResponse approve(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        AuthDirectory.SessionUser admin = authDirectory.requireAdmin(authorization);
        return approvalService.approve(admin.getUserId(), id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an approval request")
    public ApprovalRequestResponse reject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        AuthDirectory.SessionUser admin = authDirectory.requireAdmin(authorization);
        return approvalService.reject(admin.getUserId(), id, reason);
    }
}
