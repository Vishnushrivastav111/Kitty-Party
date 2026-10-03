package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.FeedbackHistoryResponse;
import com.microvault.admin.dto.FeedbackRequest;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.service.AdminService;
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
@RequestMapping("/api/feedback")
@Tag(name = "Feedback")
public class FeedbackController {

    private final AdminService adminService;
    private final AuthDirectory authDirectory;

    public FeedbackController(AdminService adminService, AuthDirectory authDirectory) {
        this.adminService = adminService;
        this.authDirectory = authDirectory;
    }

    @PostMapping
    @Operation(summary = "Send feedback")
    public ResponseEntity<FeedbackResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.addFeedback(authDirectory.requireSession(authorization), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update feedback")
    public FeedbackResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable UUID id,
                                   @RequestBody FeedbackRequest request) {
        return adminService.updateFeedback(authDirectory.requireSession(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete feedback")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        adminService.deleteFeedback(authDirectory.requireSession(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Feedback history")
    public List<FeedbackHistoryResponse> history(@PathVariable UUID id) {
        return adminService.history(id);
    }
}
