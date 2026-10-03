package com.microvault.auth.controller;

import com.microvault.auth.dto.LoginResponse;
import com.microvault.auth.dto.OtpResponse;
import com.microvault.auth.dto.UserResponse;
import com.microvault.auth.exception.DuplicateResourceException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void loginReturnsToken() throws Exception {
        UserResponse user = new UserResponse();
        user.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        user.setFullName("Aarav Sharma");
        user.setEmail("aarav.sharma@example.com");
        user.setRole("user");

        LoginResponse response = new LoginResponse();
        response.setOk(true);
        response.setToken("token-1");
        response.setUser(user);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aarav.sharma@example.com\",\"password\":\"User@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.token").value("token-1"))
                .andExpect(jsonPath("$.user.fullName").value("Aarav Sharma"));
    }

    @Test
    void loginRejectsUnknownUser() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aarav.sharma@example.com\",\"password\":\"Wrong@123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerRejectsBlankBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        UserResponse user = new UserResponse();
        user.setFullName("Priya Patel");
        user.setEmail("priya.patel@example.com");
        when(authService.register(any())).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Priya Patel\",\"email\":\"priya.patel@example.com\",\"phone\":\"9123456780\",\"password\":\"User@1234\",\"confirmPassword\":\"User@1234\",\"termsAccepted\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fullName").value("Priya Patel"));
    }

    @Test
    void registerConflictWhenEmailExists() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Priya Patel\",\"email\":\"priya.patel@example.com\",\"phone\":\"9123456780\",\"password\":\"User@1234\",\"termsAccepted\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void sendOtpReturnsDemoCodeWhenMailIsOff() throws Exception {
        OtpResponse.OtpData data = new OtpResponse.OtpData();
        data.setDemoOtp("123456");
        data.setEmail("aarav.sharma@example.com");
        OtpResponse response = new OtpResponse();
        response.setOk(true);
        response.setData(data);
        response.setMessage("Email is not configured, so the code is shown for local use only.");
        when(authService.sendResetOtp(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/forgot/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aarav.sharma@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.demoOtp").value("123456"));
    }
}
