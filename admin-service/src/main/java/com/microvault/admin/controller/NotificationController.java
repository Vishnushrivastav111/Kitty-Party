package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.NotificationResponse;
import com.microvault.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final AdminService adminService;
    private final AuthDirectory authDirectory;

    public NotificationController(AdminService adminService, AuthDirectory authDirectory) {
        this.adminService = adminService;
        this.authDirectory = authDirectory;
    }

    @GetMapping
    @Operation(summary = "List my notifications")
    public List<NotificationResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return adminService.notifications(authDirectory.requireSession(authorization).getUserId());
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<Void> readAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.markAllRead(authDirectory.requireSession(authorization).getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<Void> read(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable UUID id) {
        adminService.markRead(authDirectory.requireSession(authorization).getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a notification")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        adminService.deleteNotification(authDirectory.requireSession(authorization).getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every notification")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.deleteAllNotifications(authDirectory.requireSession(authorization).getUserId());
        return ResponseEntity.noContent().build();
    }
}
