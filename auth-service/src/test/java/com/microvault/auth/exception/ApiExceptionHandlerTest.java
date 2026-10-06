package com.microvault.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");

    @Test
    void methodArgumentNotValidJoinsEveryFieldMessage() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "body");
        result.addError(new FieldError("body", "email", "Email is required"));
        result.addError(new FieldError("body", "password", "Password is required"));
        result.addError(new ObjectError("body", "global errors are ignored"));

        ResponseEntity<ErrorResponse> response = handler.validation(argumentNotValid(result), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Email is required; Password is required", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void methodArgumentNotValidWithASingleFieldHasNoSeparator() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "body");
        result.addError(new FieldError("body", "email", "Email is required"));

        assertEquals("Email is required", handler.validation(argumentNotValid(result), request).getBody().getMessage());
    }

    @Test
    void methodArgumentNotValidWithoutFieldErrorsHasAnEmptyMessage() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "body");

        assertEquals("", handler.validation(argumentNotValid(result), request).getBody().getMessage());
    }

    @Test
    void validationExceptionMapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.badRequest(new ValidationException("Bad input"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad input", response.getBody().getMessage());
    }

    @Test
    void unreadableBodyMapsTo400WithAGenericMessage() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.unreadable(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request body is not valid", response.getBody().getMessage());
    }

    @Test
    void unauthorizedMapsTo401() {
        ResponseEntity<ErrorResponse> response = handler.unauthorized(new UnauthorizedException("Login is required"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Login is required", response.getBody().getMessage());
    }

    @Test
    void forbiddenMapsTo403() {
        ResponseEntity<ErrorResponse> response = handler.forbidden(new ForbiddenException("Not allowed"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Not allowed", response.getBody().getMessage());
    }

    @Test
    void notFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response = handler.notFound(new ResourceNotFoundException("User not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void duplicateMapsTo409() {
        ResponseEntity<ErrorResponse> response = handler.conflict(new DuplicateResourceException("Email is already registered"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Email is already registered", response.getBody().getMessage());
    }

    @Test
    void dataIntegrityViolationMapsTo409WithAGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.dataConflict(new DataIntegrityViolationException("unique constraint"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This record conflicts with an existing one", response.getBody().getMessage());
    }

    @Test
    void mailFailureMapsTo500WithAHelpfulMessage() {
        ResponseEntity<ErrorResponse> response = handler.mailFailed(new MailSendException("smtp down"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Could not send the verification email. Check mail settings.", response.getBody().getMessage());
    }

    @Test
    void anyOtherExceptionMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> response = handler.unexpected(new IllegalStateException("secret internals"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    private MethodArgumentNotValidException argumentNotValid(BeanPropertyBindingResult result) throws NoSuchMethodException {
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("dummy", Object.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), result);
    }

    @SuppressWarnings("unused")
    private void dummy(Object body) {
        // only used to build a MethodParameter
    }
}
