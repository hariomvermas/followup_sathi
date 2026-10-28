package com.followupsaathi.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;

/** Turns exceptions into the shared JSON error shape; logs unexpected ones. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex,
            HttpServletRequest request) {
        return build(HttpStatus.valueOf(ex.getStatus()), ex.getMessage(), request, null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                        .add(error.getDefaultMessage() == null
                                ? "Invalid value" : error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields",
                request, fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(),
                ex.getMessage());
        return build(HttpStatus.CONFLICT,
                "That change conflicts with existing data", request, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Malformed request body or parameters", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
            HttpServletRequest request, Map<String, List<String>> fieldErrors) {
        ErrorResponse response = new ErrorResponse(status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(),
                java.time.Instant.now(), fieldErrors);
        return ResponseEntity.status(status).body(response);
    }
}
