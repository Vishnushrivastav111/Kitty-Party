package com.microvault.auth;

import com.microvault.auth.dto.ApiDataResponse;
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
import com.microvault.auth.exception.ErrorResponse;
import com.microvault.auth.exception.ForbiddenException;
import com.microvault.auth.exception.ResourceNotFoundException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.support.BeanVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every DTO and entity must hand back exactly what was put in through its setters. */
class ModelClassesTest {

    @Test
    void dtosRoundTripEveryProperty() throws Exception {
        List<Class<?>> dtos = List.of(
                ApiDataResponse.class, ChangePasswordRequest.class, CreateUserRequest.class,
                ForgotPasswordRequest.class, LoginRequest.class, LoginResponse.class, OtpResponse.class,
                OtpResponse.OtpData.class, RegisterRequest.class, ResetPasswordRequest.class,
                SessionResponse.class, UpdateUserRequest.class, UserResponse.class);
        for (Class<?> dto : dtos) {
            assertTrue(BeanVerifier.verify(dto) > 0, dto.getSimpleName());
        }
    }

    @Test
    void userEntityRoundTripsEveryProperty() throws Exception {
        assertTrue(BeanVerifier.verify(User.class) >= 10);
    }

    @Test
    void apiDataResponseOfMarksTheResponseAsOk() {
        ApiDataResponse response = ApiDataResponse.of("payload");

        assertTrue(response.isOk());
        assertEquals("payload", response.getData());
        assertEquals(null, response.getMessage());
        assertFalse(new ApiDataResponse().isOk());
    }

    @Test
    void errorResponseCarriesStatusMessageAndPath() {
        ErrorResponse error = ErrorResponse.of(404, "missing", "/api/x");

        assertEquals(404, error.getStatus());
        assertEquals("missing", error.getMessage());
        assertEquals("/api/x", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void exceptionsKeepTheirMessage() {
        assertEquals("a", new DuplicateResourceException("a").getMessage());
        assertEquals("b", new ForbiddenException("b").getMessage());
        assertEquals("c", new ResourceNotFoundException("c").getMessage());
        assertEquals("d", new UnauthorizedException("d").getMessage());
        assertEquals("e", new ValidationException("e").getMessage());
    }
}
