package com.microvault.auth.serviceimpl;

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
import com.microvault.auth.entity.User;
import com.microvault.auth.exception.DuplicateResourceException;
import com.microvault.auth.exception.ForbiddenException;
import com.microvault.auth.exception.ResourceNotFoundException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.repository.UserRepository;
import com.microvault.auth.service.AuthService;
import com.microvault.auth.service.EmailService;
import com.microvault.auth.service.OtpStore;
import com.microvault.auth.service.TokenStore;
import com.microvault.auth.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;
    private final OtpStore otpStore;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           TokenStore tokenStore,
                           OtpStore otpStore,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
        this.otpStore = otpStore;
        this.emailService = emailService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new ValidationException("Please accept the terms to create an account");
        }
        if (request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone().trim());
        user.setRole("user");
        user.setStatus("active");
        validatePerson(user);
        validatePassword(request.getPassword());
        ensureEmailIsFree(user.getEmail(), null);

        user.setId(UUID.randomUUID());
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        return toResponse(userRepository.save(user));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.getEmail().trim())
                .orElse(null);
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("This account is inactive. Please contact an admin.");
        }

        LoginResponse response = new LoginResponse();
        response.setOk(true);
        response.setToken(tokenStore.issue(user.getId()));
        response.setUser(toResponse(user));
        response.setMessage("Login successful");
        return response;
    }

    @Override
    public void logout(String authorization) {
        tokenStore.revoke(authorization);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse session(String authorization) {
        User user = loadActive(requireUserId(authorization));
        SessionResponse response = new SessionResponse();
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        return response;
    }

    @Override
    public OtpResponse sendResetOtp(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.getEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        otpStore.save(user.getEmail(), code);
        boolean sent = emailService.sendOtp(user.getEmail(), code);

        OtpResponse.OtpData data = new OtpResponse.OtpData();
        data.setEmail(user.getEmail());
        data.setExpiresInMinutes(10);
        if (!sent) {
            data.setDemoOtp(code);
        }

        OtpResponse response = new OtpResponse();
        response.setOk(true);
        response.setData(data);
        if (sent) {
            response.setMessage("Verification code sent to your email.");
        } else {
            response.setMessage("Email is not configured, so the code is shown for local use only.");
        }
        return response;
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        validatePassword(request.getPassword());

        User user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.getEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (!otpStore.matches(user.getEmail(), request.getOtp().trim())) {
            throw new ValidationException("Verification code is invalid or expired");
        }

        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        otpStore.clear(user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(String authorization) {
        return toResponse(loadActive(requireUserId(authorization)));
    }

    @Override
    public UserResponse updateCurrentUser(String authorization, UpdateUserRequest request) {
        return updateUser(requireUserId(authorization), request);
    }

    @Override
    public void changePassword(String authorization, ChangePasswordRequest request) {
        if (request.getConfirmPassword() != null
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        User user = loadActive(requireUserId(authorization));
        if (!PasswordUtil.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is not correct");
        }
        validatePassword(request.getNewPassword());
        user.setPasswordHash(PasswordUtil.hash(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return toResponse(loadActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse lookup(UUID userId, String email) {
        if (userId != null) {
            User byId = userRepository.findByIdAndDeletedFalse(userId).orElse(null);
            if (byId != null) {
                return toResponse(byId);
            }
        }
        if (email != null && !email.isBlank()) {
            User byEmail = userRepository.findByEmailIgnoreCaseAndDeletedFalse(email.trim()).orElse(null);
            if (byEmail != null) {
                return toResponse(byEmail);
            }
        }
        throw new ResourceNotFoundException("No user found for this dashboard");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String role) {
        List<User> users;
        if (role == null || role.isBlank()) {
            users = userRepository.findByDeletedFalseOrderByCreatedAtDesc();
        } else {
            users = userRepository.findByRoleIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(role.trim());
        }
        List<UserResponse> responses = new ArrayList<>();
        for (User user : users) {
            responses.add(toResponse(user));
        }
        return responses;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone().trim());
        user.setRole(blank(request.getRole()) ? "user" : request.getRole().trim().toLowerCase());
        user.setStatus(blank(request.getStatus()) ? "active" : request.getStatus().trim().toLowerCase());
        validatePerson(user);
        validatePassword(request.getPassword());
        ensureEmailIsFree(user.getEmail(), null);

        user.setId(UUID.randomUUID());
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = loadActive(id);
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().trim().toLowerCase());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().trim().toLowerCase());
        }
        validatePerson(user);
        ensureEmailIsFree(user.getEmail(), user.getId());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validatePassword(request.getPassword());
            user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void softDelete(UUID id) {
        User user = loadActive(id);
        if ("superadmin".equalsIgnoreCase(user.getRole())) {
            throw new ValidationException("The super admin account cannot be deleted");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setDeleted(true);
        user.setDeletedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private UUID requireUserId(String authorization) {
        UUID userId = tokenStore.requireUserId(authorization);
        if (userId == null) {
            throw new UnauthorizedException("Login is required");
        }
        return userId;
    }

    private User loadActive(UUID id) {
        if (id == null) {
            throw new ValidationException("User id is required");
        }
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureEmailIsFree(String email, UUID currentId) {
        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing == null) {
            return;
        }
        if (currentId != null && currentId.equals(existing.getId())) {
            return;
        }
        throw new DuplicateResourceException("Email is already registered");
    }

    private void validatePerson(User user) {
        if (user.getFullName() == null || user.getFullName().trim().length() < 3) {
            throw new ValidationException("Full name must be at least 3 characters");
        }
        if (!user.getFullName().matches("^[A-Za-z\\s.]+$")) {
            throw new ValidationException("Name can only contain letters and spaces");
        }
        if (user.getEmail() == null || !user.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidationException("Enter a valid email address");
        }
        if (user.getPhone() == null || !user.getPhone().matches("^[6-9][0-9]{9}$")) {
            throw new ValidationException("Enter a valid 10-digit mobile number");
        }
        if (user.getRole() == null || !user.getRole().matches("user|admin|superadmin")) {
            throw new ValidationException("Role must be user, admin or superadmin");
        }
        if (user.getStatus() == null || !user.getStatus().matches("active|inactive")) {
            throw new ValidationException("Status must be active or inactive");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password needs at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password needs at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ValidationException("Password needs at least one number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new ValidationException("Password needs at least one special character");
        }
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toLocalDate().toString());
        }
        return response;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
