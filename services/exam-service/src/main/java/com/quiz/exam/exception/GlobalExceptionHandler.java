package com.quiz.exam.exception;

import com.quiz.exam.dto.BaseResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null ? fieldError.getField() + ": " + fieldError.getDefaultMessage() : "Validation error";
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<BaseResponse<Void>> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @ExceptionHandler(FeignException.BadRequest.class)
    public ResponseEntity<BaseResponse<Void>> handleFeignBadRequest(FeignException.BadRequest ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Upstream bad request");
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<BaseResponse<Void>> handleFeignUnauthorized(FeignException.Unauthorized ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<BaseResponse<Void>> handleFeignForbidden(FeignException.Forbidden ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden");
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<BaseResponse<Void>> handleFeignNotFound(FeignException.NotFound ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler({
            FeignException.ServiceUnavailable.class,
            FeignException.GatewayTimeout.class,
            FeignException.InternalServerError.class,
            FeignException.BadGateway.class
    })
    public ResponseEntity<BaseResponse<Void>> handleFeignUnavailable(FeignException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Upstream service unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("[UNEXPECTED] {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.toString(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<BaseResponse<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(BaseResponse.of(status.value(), message, null));
    }
}
