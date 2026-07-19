package com.flowset.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(
                        ex.getStatus().value(),
                        ex.getError(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        String message = fieldErrors.values().stream().findFirst().orElse("Validation Failed");
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        message,
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuth(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "Invalid email or password",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "Access denied",
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()));
    }
}
