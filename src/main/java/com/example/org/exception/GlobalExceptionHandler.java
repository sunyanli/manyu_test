package com.example.org.exception;

import com.example.org.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that translates exceptions into consistent
 * {@link ApiResponse} bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle known business exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * Handle validation errors from {@code @Valid} annotated parameters.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ApiResponse.error(400, message);
    }

    /**
     * Handle database constraint violations (unique key, etc.) as client errors.
     */
    @ExceptionHandler({DataIntegrityViolationException.class, DuplicateKeyException.class})
    public ApiResponse<Void> handleDataIntegrityViolation(Exception ex) {
        return ApiResponse.error(400, "数据已存在/冲突");
    }

    /**
     * Handle all unhandled exceptions as internal server errors.
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.error(500, "Internal Server Error");
    }
}