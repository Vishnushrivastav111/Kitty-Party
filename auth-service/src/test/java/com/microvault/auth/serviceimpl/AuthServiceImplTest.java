package com.microvault.auth.serviceimpl;

import com.microvault.auth.dto.ForgotPasswordRequest;
import com.microvault.auth.dto.LoginRequest;
import com.microvault.auth.dto.LoginResponse;
import com.microvault.auth.dto.RegisterRequest;
import com.microvault.auth.dto.ResetPasswordRequest;
import com.microvault.auth.dto.UserResponse;
import com.microvault.auth.entity.User;
import com.microvault.auth.exception.DuplicateResourceException;
import com.microvault.auth.exception.ForbiddenException;
import com.microvault.auth.exception.ResourceNotFoundException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.repository.UserRepository;
import com.microvault.auth.service.EmailService;
import com.microvault.auth.service.OtpStore;
import com.microvault.auth.service.TokenStore;
import com.microvault.auth.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private OtpStore otpStore;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        user.setFullName("Aarav Sharma");
        user.setEmail("aarav.sharma@example.com");
        user.setPhone("9876543210");
        user.setPasswordHash(PasswordUtil.hash("User@1234"));
        user.setRole("user");
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setDeleted(false);
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Priya Patel");
        request.setEmail("priya.patel@example.com");
        request.setPhone("9123456780");
        request.setPassword("User@1234");
        request.setConfirmPassword("User@1234");
        request.setTermsAccepted(true);

        when(userRepository.findByEmailIgnoreCase("priya.patel@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse saved = authService.register(request);

        assertEquals("Priya Patel", saved.getFullName());
        assertEquals("user", saved.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Aarav Sharma");
        request.setEmail("aarav.sharma@example.com");
        request.setPhone("9876543210");
        request.setPassword("User@1234");
        request.setTermsAccepted(true);

        when(userRepository.findByEmailIgnoreCase("aarav.sharma@example.com")).thenReturn(Optional.of(user));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void shouldRejectWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Priya Patel");
        request.setEmail("priya.patel@example.com");
        request.setPhone("9123456780");
        request.setPassword("short");
        request.setTermsAccepted(true);

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void shouldLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("aarav.sharma@example.com");
        request.setPassword("User@1234");

        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("aarav.sharma@example.com"))
                .thenReturn(Optional.of(user));
        when(tokenStore.issue(user.getId())).thenReturn("token-1");

        LoginResponse response = authService.login(request);

        assertTrue(response.isOk());
        assertEquals("token-1", response.getToken());
        assertEquals("Aarav Sharma", response.getUser().getFullName());
    }

    @Test
    void shouldRejectBadPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("aarav.sharma@example.com");
        request.setPassword("Wrong@123");

        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("aarav.sharma@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void shouldRejectInactiveAccount() {
        user.setStatus("inactive");
        LoginRequest request = new LoginRequest();
        request.setEmail("aarav.sharma@example.com");
        request.setPassword("User@1234");

        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("aarav.sharma@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> authService.login(request));
    }

    @Test
    void shouldFailOtpWhenEmailIsUnknown() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.sendResetOtp(request));
    }

    @Test
    void shouldRejectWrongOtp() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("aarav.sharma@example.com");
        request.setOtp("000000");
        request.setPassword("NewUser@123");
        request.setConfirmPassword("NewUser@123");

        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("aarav.sharma@example.com"))
                .thenReturn(Optional.of(user));
        when(otpStore.matches("aarav.sharma@example.com", "000000")).thenReturn(false);

        assertThrows(ValidationException.class, () -> authService.resetPassword(request));
    }

    @Test
    void shouldReturnNotFoundForMissingUser() {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.getUser(id));
    }
}
