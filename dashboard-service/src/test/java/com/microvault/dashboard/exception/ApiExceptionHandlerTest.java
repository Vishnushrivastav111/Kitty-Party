package com.microvault.dashboard.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard");

    @Test
    void validationMapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.badRequest(new ValidationException("Bad"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad", response.getBody().getMessage());
        assertEquals("/api/dashboard", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void unauthorizedMapsTo401() {
        ResponseEntity<ErrorResponse> response = handler.unauthorized(new UnauthorizedException("Login is required"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Login is required", response.getBody().getMessage());
    }

    @Test
    void missingStaticResourceMapsTo404() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/nope");

        ResponseEntity<ErrorResponse> response = handler.missing(exception, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void resourceNotFoundMapsTo404WithItsMessage() {
        ResponseEntity<ErrorResponse> response = handler.notFound(new ResourceNotFoundException("No user"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("No user", response.getBody().getMessage());
    }

    @Test
    void anyOtherExceptionMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> response = handler.unexpected(new IllegalStateException("secret"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Something went wrong", response.getBody().getMessage());
    }
}
