package com.microvault.finance.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        result.addError(new FieldError("body", "name", "Name is required"));
        result.addError(new FieldError("body", "amount", "Amount is required"));
        result.addError(new ObjectError("body", "global errors are ignored"));

        ResponseEntity<ErrorResponse> response = handler.validation(argumentNotValid(result), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Name is required; Amount is required", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void methodArgumentNotValidWithASingleFieldHasNoSeparator() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "body");
        result.addError(new FieldError("body", "name", "Name is required"));

        assertEquals("Name is required", handler.validation(argumentNotValid(result), request).getBody().getMessage());
    }

    @Test
    void methodArgumentNotValidWithoutFieldErrorsHasEmptyMessage() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "body");

        ResponseEntity<ErrorResponse> response = handler.validation(argumentNotValid(result), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("", response.getBody().getMessage());
    }

    @Test
    void validationExceptionMapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.badRequest(new ValidationException("Bad amount"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad amount", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void unreadableBodyMapsTo400WithGenericMessage() {
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
    void notFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response = handler.notFound(new ResourceNotFoundException("Goal not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Goal not found", response.getBody().getMessage());
    }

    @Test
    void duplicateMapsTo409WithItsOwnMessage() {
        ResponseEntity<ErrorResponse> response = handler.conflict(new DuplicateResourceException("Already there"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Already there", response.getBody().getMessage());
    }

    @Test
    void dataIntegrityViolationMapsTo409WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.conflict(new DataIntegrityViolationException("duplicate key value violates constraint"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This record conflicts with an existing one", response.getBody().getMessage());
    }

    @Test
    void anyOtherExceptionMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> response = handler.unexpected(new IllegalStateException("secret internals"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
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
