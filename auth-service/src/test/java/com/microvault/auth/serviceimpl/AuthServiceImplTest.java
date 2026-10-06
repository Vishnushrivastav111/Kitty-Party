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
import com.microvault.auth.service.EmailService;
import com.microvault.auth.service.OtpStore;
import com.microvault.auth.service.TokenStore;
import com.microvault.auth.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String GOOD_PASSWORD = "Str0ng@Pass";
    private static final String TOKEN = "Bearer jwt";

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private OtpStore otpStore;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl service;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ================================================================ register

    @Test
    void registerCreatesAnActiveMemberWithAHashedPassword() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());

        UserResponse response = service.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("Asha Verma", saved.getFullName());
        assertEquals("asha@example.com", saved.getEmail());
        assertEquals("9876543210", saved.getPhone());
        assertEquals("user", saved.getRole());
        assertEquals("active", saved.getStatus());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        assertTrue(saved.getPasswordHash().startsWith("pbkdf2_sha256$"));
        assertTrue(PasswordUtil.matches(GOOD_PASSWORD, saved.getPasswordHash()));

        assertEquals(saved.getId(), response.getId());
        assertEquals("Asha Verma", response.getFullName());
        assertEquals("asha@example.com", response.getEmail());
        assertEquals("user", response.getRole());
        assertEquals(saved.getCreatedAt().toLocalDate().toString(), response.getCreatedAt());
    }

    @Test
    void registerWithoutConfirmPasswordIsAccepted() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());
        RegisterRequest request = registerRequest();
        request.setConfirmPassword(null);

        assertNotNull(service.register(request));
    }

    @Test
    void registerRejectsMissingOrFalseTermsAcceptance() {
        RegisterRequest unchecked = registerRequest();
        unchecked.setTermsAccepted(false);
        RegisterRequest missing = registerRequest();
        missing.setTermsAccepted(null);

        assertEquals("Please accept the terms to create an account",
                assertThrows(ValidationException.class, () -> service.register(unchecked)).getMessage());
        assertThrows(ValidationException.class, () -> service.register(missing));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsPasswordMismatch() {
        RegisterRequest request = registerRequest();
        request.setConfirmPassword("Different@1");

        assertEquals("Passwords do not match",
                assertThrows(ValidationException.class, () -> service.register(request)).getMessage());
    }

    @Test
    void registerRejectsShortName() {
        RegisterRequest request = registerRequest();
        request.setFullName("  Al ");

        assertEquals("Full name must be at least 3 characters",
                assertThrows(ValidationException.class, () -> service.register(request)).getMessage());
    }

    @Test
    void registerRejectsNameWithDigitsOrSymbols() {
        RegisterRequest request = registerRequest();
        request.setFullName("Asha 123");

        assertEquals("Name can only contain letters and spaces",
                assertThrows(ValidationException.class, () -> service.register(request)).getMessage());
    }

    @Test
    void registerRejectsInvalidEmail() {
        RegisterRequest request = registerRequest();
        request.setEmail("not-an-email");

        assertEquals("Enter a valid email address",
                assertThrows(ValidationException.class, () -> service.register(request)).getMessage());
    }

    @Test
    void registerRejectsInvalidPhone() {
        RegisterRequest request = registerRequest();
        request.setPhone("12345");
        assertEquals("Enter a valid 10-digit mobile number",
                assertThrows(ValidationException.class, () -> service.register(request)).getMessage());

        request.setPhone("5876543210");
        assertThrows(ValidationException.class, () -> service.register(request));
    }

    @Test
    void registerRejectsWeakPasswords() {
        assertWeakPassword("Sh0rt@", "Password must be at least 8 characters");
        assertWeakPassword("lowercase1@x", "Password needs at least one uppercase letter");
        assertWeakPassword("UPPERCASE1@X", "Password needs at least one lowercase letter");
        assertWeakPassword("NoDigits@here", "Password needs at least one number");
        assertWeakPassword("NoSpecial12345", "Password needs at least one special character");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(storedUser("user")));

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.register(registerRequest()));

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerPropagatesRepositoryFailure() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(new IllegalStateException("db down"));

        RegisterRequest request = registerRequest();
        assertThrows(IllegalStateException.class, () -> service.register(request));
    }

    // ================================================================ login

    @Test
    void loginIssuesATokenForValidCredentials() {
        User user = storedUser("user");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenStore.issue(userId)).thenReturn("signed.jwt.token");

        LoginResponse response = service.login(loginRequest(" asha@example.com ", GOOD_PASSWORD));

        assertTrue(response.isOk());
        assertEquals("signed.jwt.token", response.getToken());
        assertEquals("Login successful", response.getMessage());
        assertEquals(userId, response.getUser().getId());
    }

    @Test
    void loginIsCaseInsensitiveOnTheStatus() {
        User user = storedUser("user");
        user.setStatus("ACTIVE");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenStore.issue(userId)).thenReturn("t");

        assertTrue(service.login(loginRequest("asha@example.com", GOOD_PASSWORD)).isOk());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("ghost@example.com")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> service.login(loginRequest("ghost@example.com", GOOD_PASSWORD)));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(tokenStore, never()).issue(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com"))
                .thenReturn(Optional.of(storedUser("user")));

        assertThrows(UnauthorizedException.class, () -> service.login(loginRequest("asha@example.com", "Wrong@Pass1")));
        verify(tokenStore, never()).issue(any());
    }

    @Test
    void loginRejectsAUserWithoutAStoredPasswordHash() {
        User user = storedUser("user");
        user.setPasswordHash(null);
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> service.login(loginRequest("asha@example.com", GOOD_PASSWORD)));
    }

    @Test
    void loginRejectsInactiveAccounts() {
        User user = storedUser("user");
        user.setStatus("inactive");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> service.login(loginRequest("asha@example.com", GOOD_PASSWORD)));

        assertEquals("This account is inactive. Please contact an admin.", exception.getMessage());
        verify(tokenStore, never()).issue(any());
    }

    // ================================================================ logout / session

    @Test
    void logoutRevokesTheToken() {
        service.logout(TOKEN);

        verify(tokenStore).revoke(TOKEN);
    }

    @Test
    void sessionDescribesTheLoggedInUser() {
        when(tokenStore.requireUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("admin")));

        SessionResponse session = service.session(TOKEN);

        assertEquals(userId, session.getUserId());
        assertEquals("Asha Verma", session.getFullName());
        assertEquals("asha@example.com", session.getEmail());
        assertEquals("9876543210", session.getPhone());
        assertEquals("admin", session.getRole());
        assertEquals("active", session.getStatus());
    }

    @Test
    void sessionRejectsInvalidOrExpiredTokens() {
        when(tokenStore.requireUserId("Bearer expired")).thenReturn(null);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> service.session("Bearer expired"));

        assertEquals("Login is required", exception.getMessage());
        verify(userRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void sessionRejectsATokenOfADeletedUser() {
        when(tokenStore.requireUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.session(TOKEN));
    }

    // ================================================================ forgot password

    @Test
    void sendResetOtpEmailsTheCodeWhenMailIsEnabled() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com"))
                .thenReturn(Optional.of(storedUser("user")));
        when(emailService.sendOtp(eq("asha@example.com"), anyString())).thenReturn(true);

        OtpResponse response = service.sendResetOtp(forgotRequest(" asha@example.com "));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(otpStore).save(eq("asha@example.com"), code.capture());
        assertTrue(code.getValue().matches("\\d{6}"));
        assertTrue(response.isOk());
        assertEquals("Verification code sent to your email.", response.getMessage());
        assertEquals("asha@example.com", response.getData().getEmail());
        assertEquals(10, response.getData().getExpiresInMinutes());
        assertNull(response.getData().getDemoOtp());
    }

    @Test
    void sendResetOtpReturnsTheCodeWhenMailIsDisabled() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com"))
                .thenReturn(Optional.of(storedUser("user")));
        when(emailService.sendOtp(eq("asha@example.com"), anyString())).thenReturn(false);

        OtpResponse response = service.sendResetOtp(forgotRequest("asha@example.com"));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(otpStore).save(eq("asha@example.com"), code.capture());
        assertEquals(code.getValue(), response.getData().getDemoOtp());
        assertEquals("Email is not configured, so the code is shown for local use only.", response.getMessage());
    }

    @Test
    void sendResetOtpRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.sendResetOtp(forgotRequest("ghost@example.com")));

        assertEquals("No account found with this email", exception.getMessage());
        verify(otpStore, never()).save(any(), any());
    }

    @Test
    void resetPasswordStoresTheNewHashAndClearsTheOtp() {
        User user = storedUser("user");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));
        when(otpStore.matches("asha@example.com", "123456")).thenReturn(true);

        service.resetPassword(resetRequest("asha@example.com", " 123456 ", "New@Pass123", "New@Pass123"));

        assertTrue(PasswordUtil.matches("New@Pass123", user.getPasswordHash()));
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
        verify(otpStore).clear("asha@example.com");
    }

    @Test
    void resetPasswordWithoutConfirmPasswordIsAccepted() {
        User user = storedUser("user");
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));
        when(otpStore.matches("asha@example.com", "123456")).thenReturn(true);

        service.resetPassword(resetRequest("asha@example.com", "123456", "New@Pass123", null));

        assertTrue(PasswordUtil.matches("New@Pass123", user.getPasswordHash()));
    }

    @Test
    void resetPasswordRejectsMismatchedPasswords() {
        ResetPasswordRequest request = resetRequest("asha@example.com", "123456", "New@Pass123", "Other@Pass123");

        assertEquals("Passwords do not match",
                assertThrows(ValidationException.class, () -> service.resetPassword(request)).getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordRejectsWeakPasswords() {
        ResetPasswordRequest request = resetRequest("asha@example.com", "123456", "weak", "weak");

        assertEquals("Password must be at least 8 characters",
                assertThrows(ValidationException.class, () -> service.resetPassword(request)).getMessage());
    }

    @Test
    void resetPasswordRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("ghost@example.com")).thenReturn(Optional.empty());
        ResetPasswordRequest request = resetRequest("ghost@example.com", "123456", "New@Pass123", "New@Pass123");

        assertThrows(ResourceNotFoundException.class, () -> service.resetPassword(request));
    }

    @Test
    void resetPasswordRejectsAWrongOrExpiredCode() {
        User user = storedUser("user");
        String originalHash = user.getPasswordHash();
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com")).thenReturn(Optional.of(user));
        when(otpStore.matches("asha@example.com", "000000")).thenReturn(false);
        ResetPasswordRequest request = resetRequest("asha@example.com", "000000", "New@Pass123", "New@Pass123");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.resetPassword(request));

        assertEquals("Verification code is invalid or expired", exception.getMessage());
        assertEquals(originalHash, user.getPasswordHash());
        verify(userRepository, never()).save(any());
        verify(otpStore, never()).clear(any());
    }

    // ================================================================ current user / change password

    @Test
    void currentUserReturnsTheLoggedInUser() {
        stubSession();

        UserResponse response = service.currentUser(TOKEN);

        assertEquals(userId, response.getId());
        assertEquals("Asha Verma", response.getFullName());
    }

    @Test
    void currentUserRejectsAnInvalidToken() {
        when(tokenStore.requireUserId("bad")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> service.currentUser("bad"));
    }

    @Test
    void updateCurrentUserChangesTheProfileOfTheLoggedInUser() {
        stubSession();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Asha Kumari");
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(storedUser("user")));

        UserResponse response = service.updateCurrentUser(TOKEN, request);

        assertEquals("Asha Kumari", response.getFullName());
    }

    @Test
    void updateCurrentUserRejectsAnInvalidToken() {
        when(tokenStore.requireUserId("bad")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> service.updateCurrentUser("bad", new UpdateUserRequest()));
    }

    @Test
    void changePasswordStoresTheNewHash() {
        User user = storedUser("user");
        when(tokenStore.requireUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));

        service.changePassword(TOKEN, changeRequest(GOOD_PASSWORD, "New@Pass123", "New@Pass123"));

        assertTrue(PasswordUtil.matches("New@Pass123", user.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordWithoutConfirmPasswordIsAccepted() {
        User user = storedUser("user");
        when(tokenStore.requireUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));

        service.changePassword(TOKEN, changeRequest(GOOD_PASSWORD, "New@Pass123", null));

        assertTrue(PasswordUtil.matches("New@Pass123", user.getPasswordHash()));
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() {
        ChangePasswordRequest request = changeRequest(GOOD_PASSWORD, "New@Pass123", "Other@Pass123");

        assertEquals("Passwords do not match",
                assertThrows(ValidationException.class, () -> service.changePassword(TOKEN, request)).getMessage());
        verify(tokenStore, never()).requireUserId(any());
    }

    @Test
    void changePasswordRejectsAWrongCurrentPassword() {
        stubSession();
        ChangePasswordRequest request = changeRequest("Wrong@Pass1", "New@Pass123", "New@Pass123");

        assertEquals("Current password is not correct",
                assertThrows(ValidationException.class, () -> service.changePassword(TOKEN, request)).getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsAWeakNewPassword() {
        stubSession();
        ChangePasswordRequest request = changeRequest(GOOD_PASSWORD, "weak", "weak");

        assertThrows(ValidationException.class, () -> service.changePassword(TOKEN, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsAnInvalidToken() {
        when(tokenStore.requireUserId("bad")).thenReturn(null);
        ChangePasswordRequest request = changeRequest(GOOD_PASSWORD, "New@Pass123", "New@Pass123");

        assertThrows(UnauthorizedException.class, () -> service.changePassword("bad", request));
    }

    // ================================================================ getUser / lookup

    @Test
    void getUserReturnsTheUser() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("user")));

        assertEquals("asha@example.com", service.getUser(userId).getEmail());
    }

    @Test
    void getUserWithoutAnIdIsRejected() {
        ValidationException exception = assertThrows(ValidationException.class, () -> service.getUser(null));

        assertEquals("User id is required", exception.getMessage());
    }

    @Test
    void getUserThrowsNotFoundForUnknownId() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getUser(userId));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getUserLeavesCreatedAtEmptyWhenTheUserHasNoTimestamp() {
        User user = storedUser("user");
        user.setCreatedAt(null);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));

        assertNull(service.getUser(userId).getCreatedAt());
    }

    @Test
    void lookupFindsAUserById() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("user")));

        assertEquals(userId, service.lookup(userId, "ignored@example.com").getId());
        verify(userRepository, never()).findByEmailIgnoreCaseAndDeletedFalse(any());
    }

    @Test
    void lookupFallsBackToEmailWhenTheIdIsUnknown() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com"))
                .thenReturn(Optional.of(storedUser("user")));

        assertEquals(userId, service.lookup(userId, " asha@example.com ").getId());
    }

    @Test
    void lookupFindsAUserByEmailWhenNoIdIsGiven() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("asha@example.com"))
                .thenReturn(Optional.of(storedUser("user")));

        assertEquals(userId, service.lookup(null, "asha@example.com").getId());
    }

    @Test
    void lookupFailsWhenNeitherIdNorEmailMatches() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.lookup(userId, "ghost@example.com"));

        assertEquals("No user found for this dashboard", exception.getMessage());
    }

    @Test
    void lookupFailsWithoutIdAndWithABlankOrNullEmail() {
        assertThrows(ResourceNotFoundException.class, () -> service.lookup(null, null));
        assertThrows(ResourceNotFoundException.class, () -> service.lookup(null, "   "));
        verify(userRepository, never()).findByEmailIgnoreCaseAndDeletedFalse(any());
    }

    // ================================================================ listUsers

    @Test
    void listUsersWithoutRoleReturnsEveryActiveUser() {
        when(userRepository.findByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(storedUser("user"), storedUser("admin")));

        List<UserResponse> users = service.listUsers(null);

        assertEquals(2, users.size());
        assertEquals("admin", users.get(1).getRole());
        verify(userRepository, never()).findByRoleIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(any());
    }

    @Test
    void listUsersWithBlankRoleReturnsEveryActiveUser() {
        when(userRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());

        assertTrue(service.listUsers("   ").isEmpty());
    }

    @Test
    void listUsersFiltersByTrimmedRole() {
        when(userRepository.findByRoleIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("admin"))
                .thenReturn(List.of(storedUser("admin")));

        List<UserResponse> users = service.listUsers(" admin ");

        assertEquals(1, users.size());
        assertEquals("admin", users.get(0).getRole());
    }

    @Test
    void listUsersPropagatesRepositoryFailure() {
        when(userRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.listUsers(null));
    }

    // ================================================================ createUser

    @Test
    void createUserDefaultsToAnActiveMember() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());

        UserResponse response = service.createUser(createRequest(null, null));

        assertEquals("user", response.getRole());
        assertEquals("active", response.getStatus());
    }

    @Test
    void createUserDefaultsWhenRoleAndStatusAreBlank() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());

        UserResponse response = service.createUser(createRequest("  ", " "));

        assertEquals("user", response.getRole());
        assertEquals("active", response.getStatus());
    }

    @Test
    void createUserNormalisesRoleAndStatus() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());

        UserResponse response = service.createUser(createRequest(" ADMIN ", " Inactive "));

        assertEquals("admin", response.getRole());
        assertEquals("inactive", response.getStatus());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(PasswordUtil.matches(GOOD_PASSWORD, captor.getValue().getPasswordHash()));
    }

    @Test
    void createUserRejectsAnUnknownRole() {
        assertEquals("Role must be user, admin or superadmin",
                assertThrows(ValidationException.class, () -> service.createUser(createRequest("owner", null))).getMessage());
    }

    @Test
    void createUserRejectsAnUnknownStatus() {
        assertEquals("Status must be active or inactive",
                assertThrows(ValidationException.class, () -> service.createUser(createRequest(null, "banned"))).getMessage());
    }

    @Test
    void createUserRejectsAWeakPassword() {
        CreateUserRequest request = createRequest(null, null);
        request.setPassword("weak");

        assertThrows(ValidationException.class, () -> service.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsADuplicateEmail() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(storedUser("user")));

        assertThrows(DuplicateResourceException.class, () -> service.createUser(createRequest(null, null)));
        verify(userRepository, never()).save(any());
    }

    // ================================================================ updateUser

    @Test
    void updateUserChangesEveryProvidedField() {
        User user = storedUser("user");
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName(" New Name ");
        request.setEmail(" NEW@Example.com ");
        request.setPhone(" 9123456780 ");
        request.setRole(" Admin ");
        request.setStatus(" Inactive ");
        request.setPassword("New@Pass123");

        UserResponse response = service.updateUser(userId, request);

        assertEquals("New Name", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("9123456780", user.getPhone());
        assertEquals("admin", user.getRole());
        assertEquals("inactive", user.getStatus());
        assertTrue(PasswordUtil.matches("New@Pass123", user.getPasswordHash()));
        assertNotNull(user.getUpdatedAt());
        assertEquals("New Name", response.getFullName());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserWithNoFieldsChangesNothing() {
        User user = storedUser("user");
        String hash = user.getPasswordHash();
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));

        service.updateUser(userId, new UpdateUserRequest());

        assertEquals("Asha Verma", user.getFullName());
        assertEquals("user", user.getRole());
        assertEquals("active", user.getStatus());
        assertEquals(hash, user.getPasswordHash());
    }

    @Test
    void updateUserIgnoresBlankRoleStatusAndPassword() {
        User user = storedUser("user");
        String hash = user.getPasswordHash();
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("  ");
        request.setStatus("");
        request.setPassword("   ");

        service.updateUser(userId, request);

        assertEquals("user", user.getRole());
        assertEquals("active", user.getStatus());
        assertEquals(hash, user.getPasswordHash());
    }

    @Test
    void updateUserRejectsAnEmailOwnedByAnotherUser() {
        User user = storedUser("user");
        User other = storedUser("user");
        other.setId(UUID.randomUUID());
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(other));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@example.com");

        assertThrows(DuplicateResourceException.class, () -> service.updateUser(userId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRejectsAWeakPassword() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("user")));
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("weak");

        assertThrows(ValidationException.class, () -> service.updateUser(userId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRejectsInvalidRoleAndStatus() {
        when(userRepository.findByIdAndDeletedFalse(userId))
                .thenAnswer(invocation -> Optional.of(storedUser("user")));
        UpdateUserRequest badRole = new UpdateUserRequest();
        badRole.setRole("owner");
        UpdateUserRequest badStatus = new UpdateUserRequest();
        badStatus.setStatus("banned");

        assertEquals("Role must be user, admin or superadmin",
                assertThrows(ValidationException.class, () -> service.updateUser(userId, badRole)).getMessage());
        assertEquals("Status must be active or inactive",
                assertThrows(ValidationException.class, () -> service.updateUser(userId, badStatus)).getMessage());
    }

    @Test
    void updateUserRejectsAnUnknownUser() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(userId, new UpdateUserRequest()));
    }

    @Test
    void updateUserWithoutIdIsRejected() {
        assertThrows(ValidationException.class, () -> service.updateUser(null, new UpdateUserRequest()));
    }

    @Test
    void updateUserValidatesEveryStoredFieldWhenTheStoredRecordIsIncomplete() {
        UpdateUserRequest request = new UpdateUserRequest();

        User noName = storedUser("user");
        noName.setFullName(null);
        assertStoredUserRejected(noName, request, "Full name must be at least 3 characters");

        User noEmail = storedUser("user");
        noEmail.setEmail(null);
        assertStoredUserRejected(noEmail, request, "Enter a valid email address");

        User noPhone = storedUser("user");
        noPhone.setPhone(null);
        assertStoredUserRejected(noPhone, request, "Enter a valid 10-digit mobile number");

        User noRole = storedUser("user");
        noRole.setRole(null);
        assertStoredUserRejected(noRole, request, "Role must be user, admin or superadmin");

        User noStatus = storedUser("user");
        noStatus.setStatus(null);
        assertStoredUserRejected(noStatus, request, "Status must be active or inactive");
    }

    // ================================================================ softDelete

    @Test
    void softDeleteMarksTheUserDeleted() {
        User user = storedUser("user");
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));

        service.softDelete(userId);

        assertTrue(user.isDeleted());
        assertNotNull(user.getDeletedAt());
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void softDeleteRefusesToDeleteTheSuperAdmin() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("SuperAdmin")));

        ValidationException exception = assertThrows(ValidationException.class, () -> service.softDelete(userId));

        assertEquals("The super admin account cannot be deleted", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void softDeleteRejectsAnUnknownUser() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.softDelete(userId));
    }

    // ================================================================ helpers

    private void assertWeakPassword(String password, String message) {
        RegisterRequest request = registerRequest();
        request.setPassword(password);
        request.setConfirmPassword(password);
        assertEquals(message, assertThrows(ValidationException.class, () -> service.register(request)).getMessage());
    }

    private void assertStoredUserRejected(User stored, UpdateUserRequest request, String message) {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(stored));
        assertEquals(message, assertThrows(ValidationException.class, () -> service.updateUser(userId, request)).getMessage());
    }

    private void stubSession() {
        when(tokenStore.requireUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(storedUser("user")));
    }

    private User storedUser(String role) {
        User user = new User();
        user.setId(userId);
        user.setFullName("Asha Verma");
        user.setEmail("asha@example.com");
        user.setPhone("9876543210");
        user.setPasswordHash(PasswordUtil.hash(GOOD_PASSWORD));
        user.setRole(role);
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 10, 9, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 1, 10, 9, 0));
        return user;
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("  Asha Verma ");
        request.setEmail("  Asha@Example.com ");
        request.setPhone(" 9876543210 ");
        request.setPassword(GOOD_PASSWORD);
        request.setConfirmPassword(GOOD_PASSWORD);
        request.setTermsAccepted(true);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private ForgotPasswordRequest forgotRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private ResetPasswordRequest resetRequest(String email, String otp, String password, String confirm) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setOtp(otp);
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        return request;
    }

    private ChangePasswordRequest changeRequest(String current, String next, String confirm) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(next);
        request.setConfirmPassword(confirm);
        return request;
    }

    private CreateUserRequest createRequest(String role, String status) {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName(" Asha Verma ");
        request.setEmail("Asha@Example.com");
        request.setPhone("9876543210");
        request.setPassword(GOOD_PASSWORD);
        request.setRole(role);
        request.setStatus(status);
        return request;
    }
}
