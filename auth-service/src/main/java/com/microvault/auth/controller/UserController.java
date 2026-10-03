package com.microvault.auth.controller;

import com.microvault.auth.dto.ApiDataResponse;
import com.microvault.auth.dto.ChangePasswordRequest;
import com.microvault.auth.dto.CreateUserRequest;
import com.microvault.auth.dto.SessionResponse;
import com.microvault.auth.dto.UpdateUserRequest;
import com.microvault.auth.dto.UserResponse;
import com.microvault.auth.exception.ForbiddenException;
import com.microvault.auth.service.AuthService;
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile and administration")
public class UserController {

    private final AuthService authService;
    private final String internalToken;

    public UserController(AuthService authService,
                          @Value("${microvault.internal.token}") String internalToken) {
        this.authService = authService;
        this.internalToken = internalToken;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Find a user for the dashboard", description = "Looks up an active user by id or email. A member can only read their own account. Admins can look up another user.")
    public UserResponse lookup(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestParam(required = false) UUID userId,
                               @RequestParam(required = false) String email) {
        SessionResponse session = authService.session(authorization);
        if (isAdmin(session.getRole())) {
            if (userId == null && (email == null || email.isBlank())) {
                return authService.currentUser(authorization);
            }
            return authService.lookup(userId, email);
        }
        if (userId != null && !userId.equals(session.getUserId())) {
            throw new ForbiddenException("You can only access your own account");
        }
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(session.getEmail())) {
            throw new ForbiddenException("You can only access your own account");
        }
        return authService.currentUser(authorization);
    }

    @GetMapping
    @Operation(summary = "List users", description = "Returns active users. Optional role filter: user, admin or superadmin.")
    public List<UserResponse> list(@RequestParam(required = false) String role,
                                   @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        return authService.listUsers(role);
    }

    @GetMapping("/me")
    @Operation(summary = "My profile")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.currentUser(authorization);
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile")
    public UserResponse updateMe(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody UpdateUserRequest request) {
        return authService.updateCurrentUser(authorization, request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change my password")
    public ResponseEntity<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authorization, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public UserResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @PathVariable UUID id) {
        SessionResponse session = authService.session(authorization);
        if (!isAdmin(session.getRole()) && !session.getUserId().equals(id)) {
            throw new ForbiddenException("You can only access your own account");
        }
        return authService.getUser(id);
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Used by admin flows to create an admin account.")
    public ResponseEntity<ApiDataResponse> create(@Valid @RequestBody CreateUserRequest request,
                                                   @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiDataResponse.of(authService.createUser(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ApiDataResponse update(@PathVariable UUID id,
                                  @RequestBody UpdateUserRequest request,
                                  @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        return ApiDataResponse.of(authService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a user")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        authService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(String role) {
        return "admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }

    private void requireInternal(String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new com.microvault.auth.exception.UnauthorizedException("Login is required");
        }
    }
}
