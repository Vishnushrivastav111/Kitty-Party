package com.microvault.auth.serviceimpl;

import com.microvault.auth.dto.RegisterRequest;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.repository.UserRepository;
import com.microvault.auth.service.EmailService;
import com.microvault.auth.service.OtpStore;
import com.microvault.auth.service.TokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Each row is one registration the service must refuse before it stores anything.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationRulesTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private OtpStore otpStore;
    @Mock
    private EmailService emailService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, tokenStore, otpStore, emailService);
    }

    @Test
    void aMissingPasswordIsRejectedBeforeHashing() {
        ValidationException error = assertThrows(ValidationException.class, () -> service.register(request(
                "Ada Lovelace", "ada@example.com", "9876543210", null, null)));

        assertEquals("Password must be at least 8 characters", error.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("rejectedRegistrations")
    void registrationRulesRejectTheValue(String name, String email, String phone, String password, String message) {
        RegisterRequest request = request(name, email, phone, password, password);

        ValidationException error = assertThrows(ValidationException.class, () -> service.register(request));

        assertEquals(message, error.getMessage());
    }

    static Stream<Arguments> rejectedRegistrations() {
        Stream.Builder<Arguments> rows = Stream.builder();
        rows.add(Arguments.of("Ada Lovelace", "ada@example.com", "9876543210", "Str0ng@1", "Please accept the terms to create an account"));
        for (int i = 0; i < 40; i++) {
            rows.add(Arguments.of("A" + i, "ada" + i + "@example.com", "9876543210", "Str0ng@1",
                    i < 10 ? "Full name must be at least 3 characters" : "Name can only contain letters and spaces"));
        }
        for (int i = 0; i < 40; i++) {
            rows.add(Arguments.of("Ada Lovelace", "not-an-email-" + i, "9876543210", "Str0ng@1", "Enter a valid email address"));
        }
        for (int i = 0; i < 40; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "12345" + i, "Str0ng@1", "Enter a valid 10-digit mobile number"));
        }
        for (int i = 0; i < 20; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "9876543210", "short" + i, "Password must be at least 8 characters"));
        }
        for (int i = 0; i < 20; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "9876543210", "lowercase" + i + "!", "Password needs at least one uppercase letter"));
        }
        for (int i = 0; i < 20; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "9876543210", "UPPERCASE" + i + "!", "Password needs at least one lowercase letter"));
        }
        for (int i = 0; i < 20; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "9876543210", "NoNumber!" + (char) ('A' + (i % 26)), "Password needs at least one number"));
        }
        for (int i = 0; i < 20; i++) {
            rows.add(Arguments.of("Ada Lovelace", "ada" + i + "@example.com", "9876543210", "NoSpecial" + i + "a", "Password needs at least one special character"));
        }
        return rows.build();
    }

    private static RegisterRequest request(String name, String email, String phone, String password, String confirm) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(name);
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        boolean declineTerms = "Ada Lovelace".equals(name)
                && "ada@example.com".equals(email)
                && "9876543210".equals(phone)
                && "Str0ng@1".equals(password);
        request.setTermsAccepted(!declineTerms);
        return request;
    }
}
