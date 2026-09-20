package com.vijay.todo_management.exception;

import com.vijay.todo_management.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleValidationExceptions_ReturnsBadRequestWithFieldErrors() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "must not be blank"));
        bindingResult.addError(new FieldError("target", "email", "invalid email address"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Validation failed for one or more fields", response.getBody().getMessage());
        assertNotNull(response.getBody().getFieldErrors());
        assertEquals(2, response.getBody().getFieldErrors().size());
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("name"));
        assertEquals("invalid email address", response.getBody().getFieldErrors().get("email"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleHttpMessageNotReadable_ReturnsBadRequestWithoutStackOrJacksonLeaks() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type...",
                (org.springframework.http.HttpInputMessage) null
        );

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Malformed JSON request payload", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleBadRequestException_Returns400() {
        BadRequestException ex = new BadRequestException("Custom bad request message");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Custom bad request message", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleForbiddenException_Returns403() {
        ForbiddenException ex = new ForbiddenException("Access denied: caller is not a member of this project or workspace");

        ResponseEntity<ErrorResponse> response = handler.handleForbiddenException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Access denied: caller is not a member of this project or workspace", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleResourceNotFoundException_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Workspace not found with slug: my-workspace");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Workspace not found with slug: my-workspace", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleResourceConflictException_Returns409() {
        ResourceConflictException ex = new ResourceConflictException("User is already a member of this project");

        ResponseEntity<ErrorResponse> response = handler.handleResourceConflictException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("User is already a member of this project", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleGenericException_Returns500WithoutLeakingStackOrDetails() {
        Exception ex = new NullPointerException("Secret internal database pointer null");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("Secret"));
        assertFalse(response.getBody().getMessage().contains("NullPointerException"));
        assertNull(response.getBody().getFieldErrors());
        assertNotNull(response.getBody().getTimestamp());
    }
}
