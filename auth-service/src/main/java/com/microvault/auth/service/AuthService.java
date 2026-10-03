package com.microvault.auth.service;

import com.microvault.auth.dto.ChangePasswordRequest;
import com.microvault.auth.dto.CreateUserRequest;
import com.microvault.auth.dto.ForgotPasswordRequest;
import com.microvault.auth.dto.LoginRequest;
import com.microvault.auth.dto.LoginResponse;
import com.microvault.auth.dto.OtpResponse;
import com.microvault.auth.dto.RegisterRequest;
import com.microvault.auth.dto.ResetPasswordRequest;
import com.microvault.auth.dto.SessionResponse;
import com.microvault.auth.dto.UpdateUserRequest;
import com.microvault.auth.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String authorization);

    SessionResponse session(String authorization);

    OtpResponse sendResetOtp(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse currentUser(String authorization);

    UserResponse updateCurrentUser(String authorization, UpdateUserRequest request);

    void changePassword(String authorization, ChangePasswordRequest request);

    UserResponse getUser(UUID id);

    UserResponse lookup(UUID userId, String email);

    List<UserResponse> listUsers(String role);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void softDelete(UUID id);
}
