package com.microvault.auth.controller;

import com.microvault.auth.dto.ApiDataResponse;
import com.microvault.auth.dto.ForgotPasswordRequest;
import com.microvault.auth.dto.LoginRequest;
import com.microvault.auth.dto.LoginResponse;
import com.microvault.auth.dto.OtpResponse;
import com.microvault.auth.dto.RegisterRequest;
import com.microvault.auth.dto.ResetPasswordRequest;
import com.microvault.auth.dto.SessionResponse;
import com.microvault.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Register, login and password reset")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a member", description = "Creates an active user account. Passwords are stored as a PBKDF2 hash.")
    public ResponseEntity<ApiDataResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiDataResponse.of(authService.register(request)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in", description = "Checks email and password and returns a signed JWT.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Invalidates the current JWT.")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    @Operation(summary = "Current session", description = "Used by other services to read the logged-in user from a JWT.")
    public SessionResponse session(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.session(authorization);
    }

    @PostMapping("/forgot/send-otp")
    @SecurityRequirements
    @Operation(summary = "Send password reset code", description = "Emails a 6-digit code. When mail is disabled, the response includes demoOtp for local use.")
    public OtpResponse sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.sendResetOtp(request);
    }

    @PostMapping("/forgot/reset")
    @SecurityRequirements
    @Operation(summary = "Reset password with OTP", description = "Sets a new password when the email and verification code match.")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
