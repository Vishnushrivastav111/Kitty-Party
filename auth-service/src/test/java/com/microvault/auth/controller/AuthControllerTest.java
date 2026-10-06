package com.microvault.auth.controller;

import com.microvault.auth.dto.ForgotPasswordRequest;
import com.microvault.auth.dto.LoginRequest;
import com.microvault.auth.dto.LoginResponse;
import com.microvault.auth.dto.OtpResponse;
import com.microvault.auth.dto.RegisterRequest;
import com.microvault.auth.dto.ResetPasswordRequest;
import com.microvault.auth.dto.SessionResponse;
import com.microvault.auth.dto.UserResponse;
import com.microvault.auth.exception.DuplicateResourceException;
import com.microvault.auth.exception.ForbiddenException;
import com.microvault.auth.exception.ResourceNotFoundException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String REGISTER_BODY = "{\"fullName\":\"Asha Verma\",\"email\":\"asha@example.com\","
            + "\"phone\":\"9876543210\",\"password\":\"Str0ng@Pass\",\"confirmPassword\":\"Str0ng@Pass\",\"termsAccepted\":true}";
    private static final String LOGIN_BODY = "{\"email\":\"asha@example.com\",\"password\":\"Str0ng@Pass\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    // ---------------------------------------------------------------- register

    @Test
    void registerReturnsCreatedWithTheNewUser() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(user());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.email").value("asha@example.com"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertEquals("Asha Verma", captor.getValue().getFullName());
        assertEquals("9876543210", captor.getValue().getPhone());
        assertEquals(Boolean.TRUE, captor.getValue().getTermsAccepted());
    }

    @Test
    void registerRejectsAnEmptyBodyWithEveryFieldMessage() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Full name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Email is required")))
                .andExpect(jsonPath("$.message").value(containsString("Phone is required")))
                .andExpect(jsonPath("$.message").value(containsString("Password is required")));
        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsAnInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("asha@example.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid email address"));
    }

    @Test
    void registerRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is not valid"));
    }

    @Test
    void registerMapsBusinessValidationTo400() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ValidationException("Passwords do not match"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void registerMapsDuplicateEmailTo409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void registerMapsUnexpectedFailureTo500() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    // ---------------------------------------------------------------- login

    @Test
    void loginReturnsTheToken() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setOk(true);
        response.setToken("token-1");
        response.setUser(user());
        response.setMessage("Login successful");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.token").value("token-1"))
                .andExpect(jsonPath("$.user.fullName").value("Asha Verma"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void loginRejectsBadCredentialsWith401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void loginRejectsAnInactiveAccountWith403() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ForbiddenException("This account is inactive. Please contact an admin."));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("This account is inactive. Please contact an admin."));
    }

    @Test
    void loginRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email is required")))
                .andExpect(jsonPath("$.message").value(containsString("Password is required")));
        verifyNoInteractions(authService);
    }

    @Test
    void loginRejectsAMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid email address"));
    }

    // ---------------------------------------------------------------- logout / session

    @Test
    void logoutRevokesTheTokenAndReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer jwt"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authService).logout("Bearer jwt");
    }

    @Test
    void logoutWithoutATokenStillReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());

        verify(authService).logout(isNull());
    }

    @Test
    void sessionReturnsTheLoggedInUser() throws Exception {
        SessionResponse session = new SessionResponse();
        session.setUserId(userId);
        session.setFullName("Asha Verma");
        session.setEmail("asha@example.com");
        session.setRole("user");
        session.setStatus("active");
        when(authService.session("Bearer jwt")).thenReturn(session);

        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    void sessionWithoutATokenIsUnauthorized() throws Exception {
        when(authService.session(isNull())).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
    }

    @Test
    void sessionWithAnExpiredOrMalformedTokenIsUnauthorized() throws Exception {
        when(authService.session("Bearer expired")).thenThrow(new UnauthorizedException("Login is required"));
        when(authService.session("garbage")).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/session").header("Authorization", "garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionOfADeletedUserIsNotFound() throws Exception {
        when(authService.session("Bearer jwt")).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer jwt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ---------------------------------------------------------------- forgot password

    @Test
    void sendOtpReturnsTheOtpEnvelope() throws Exception {
        OtpResponse response = new OtpResponse();
        response.setOk(true);
        response.setMessage("Verification code sent to your email.");
        OtpResponse.OtpData data = new OtpResponse.OtpData();
        data.setEmail("asha@example.com");
        data.setExpiresInMinutes(10);
        response.setData(data);
        when(authService.sendResetOtp(any(ForgotPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/forgot/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.email").value("asha@example.com"))
                .andExpect(jsonPath("$.data.expiresInMinutes").value(10));
    }

    @Test
    void sendOtpForAnUnknownEmailIs404() throws Exception {
        when(authService.sendResetOtp(any(ForgotPasswordRequest.class)))
                .thenThrow(new ResourceNotFoundException("No account found with this email"));

        mockMvc.perform(post("/api/auth/forgot/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No account found with this email"));
    }

    @Test
    void sendOtpRejectsAMissingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot/send-otp").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    void sendOtpMapsAMailFailureTo500WithAHelpfulMessage() throws Exception {
        when(authService.sendResetOtp(any(ForgotPasswordRequest.class))).thenThrow(new MailSendException("smtp down"));

        mockMvc.perform(post("/api/auth/forgot/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Could not send the verification email. Check mail settings."));
    }

    @Test
    void resetPasswordReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\",\"otp\":\"123456\",\"password\":\"New@Pass123\",\"confirmPassword\":\"New@Pass123\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ResetPasswordRequest> captor = ArgumentCaptor.forClass(ResetPasswordRequest.class);
        verify(authService).resetPassword(captor.capture());
        assertEquals("123456", captor.getValue().getOtp());
        assertEquals("New@Pass123", captor.getValue().getPassword());
    }

    @Test
    void resetPasswordRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/forgot/reset").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Verification code is required")))
                .andExpect(jsonPath("$.message").value(containsString("Password is required")));
        verifyNoInteractions(authService);
    }

    @Test
    void resetPasswordMapsAnInvalidCodeTo400() throws Exception {
        doThrow(new ValidationException("Verification code is invalid or expired"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\",\"otp\":\"000000\",\"password\":\"New@Pass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code is invalid or expired"));
    }

    // ---------------------------------------------------------------- CORS (public endpoints are reachable cross-origin)

    @Test
    void corsPreflightIsAllowedOnPublicEndpoints() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private UserResponse user() {
        UserResponse user = new UserResponse();
        user.setId(userId);
        user.setFullName("Asha Verma");
        user.setEmail("asha@example.com");
        user.setPhone("9876543210");
        user.setRole("user");
        user.setStatus("active");
        return user;
    }
}
