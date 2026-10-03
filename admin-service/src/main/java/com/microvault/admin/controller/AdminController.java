package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.AdminWorkspaceResponse;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.dto.NewsRequest;
import com.microvault.admin.dto.NewsResponse;
import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthDirectory authDirectory;
    private final String internalToken;

    public AdminController(AdminService adminService,
                           AuthDirectory authDirectory,
                           @Value("${microvault.internal.token}") String internalToken) {
        this.adminService = adminService;
        this.authDirectory = authDirectory;
        this.internalToken = internalToken;
    }

    @GetMapping("/workspace")
    @Operation(summary = "Dashboard admin slice", description = "Notifications, feedback, news and, for admins, member insights.")
    public AdminWorkspaceResponse workspace(@RequestParam UUID userId,
                                            @RequestParam(required = false) String role,
                                            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new UnauthorizedException("Login is required");
        }
        return adminService.workspace(userId, role);
    }

    @GetMapping("/users")
    @Operation(summary = "List members")
    public List<UserCard> users(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authDirectory.requireAdmin(authorization);
        return adminService.members();
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update a member")
    public UserCard updateUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable UUID id,
                               @RequestBody UserWriteRequest request) {
        authDirectory.requireAdmin(authorization);
        return adminService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Soft delete a member")
    public ResponseEntity<Void> deleteUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable UUID id) {
        authDirectory.requireAdmin(authorization);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admins")
    @Operation(summary = "List admins")
    public List<UserCard> admins(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authDirectory.requireAdmin(authorization);
        return adminService.admins();
    }

    @PostMapping("/admins")
    @Operation(summary = "Create an admin")
    public ResponseEntity<UserCard> createAdmin(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @RequestBody UserWriteRequest request) {
        authDirectory.requireAdmin(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAdmin(request));
    }

    @PutMapping("/admins/{id}")
    @Operation(summary = "Update an admin")
    public UserCard updateAdmin(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable UUID id,
                                @RequestBody UserWriteRequest request) {
        authDirectory.requireAdmin(authorization);
        if (request.getRole() == null || request.getRole().isBlank()) {
            request.setRole("admin");
        }
        return adminService.updateUser(id, request);
    }

    @DeleteMapping("/admins/{id}")
    @Operation(summary = "Soft delete an admin")
    public ResponseEntity<Void> deleteAdmin(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable UUID id) {
        authDirectory.requireAdmin(authorization);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feedback")
    @Operation(summary = "All feedback")
    public List<FeedbackResponse> feedback(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authDirectory.requireAdmin(authorization);
        return adminService.allFeedback();
    }

    @GetMapping("/news")
    @Operation(summary = "All news")
    public List<NewsResponse> news(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authDirectory.requireAdmin(authorization);
        return adminService.allNews();
    }

    @PostMapping("/news")
    @Operation(summary = "Publish or draft a news item")
    public ResponseEntity<NewsResponse> createNews(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.addNews(authDirectory.requireAdmin(authorization), request));
    }

    @PutMapping("/news/{id}")
    @Operation(summary = "Update a news item")
    public NewsResponse updateNews(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable UUID id,
                                   @RequestBody NewsRequest request) {
        authDirectory.requireAdmin(authorization);
        return adminService.updateNews(id, request);
    }

    @DeleteMapping("/news/{id}")
    @Operation(summary = "Soft delete a news item")
    public ResponseEntity<Void> deleteNews(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable UUID id) {
        authDirectory.requireAdmin(authorization);
        adminService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
