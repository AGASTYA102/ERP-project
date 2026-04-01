package com.erp.manufacturing.controller;
 
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
 
import java.util.HashMap;
import java.util.Map;
 
@ControllerAdvice
public class GlobalExceptionHandler extends org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler {
 
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public Object handleEntityNotFound(jakarta.persistence.EntityNotFoundException ex, Model model, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        model.addAttribute("errorMessage", "The requested resource was not found.");
        return "error";
    }
 
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public Object handleConstraintViolation(jakarta.validation.ConstraintViolationException ex, Model model, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + ex.getMessage());
        }
        model.addAttribute("errorMessage", "Invalid data provided. Please check your inputs.");
        return "error";
    }
 
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, Model model, HttpServletRequest request) {
        log.warn("Type mismatch error on URI {}: {}", request.getRequestURI(), ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input format (expected " + ex.getRequiredType().getSimpleName() + ")");
        }
        model.addAttribute("errorMessage", "The input format was incorrect.");
        return "error";
    }
 
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            org.springframework.http.HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", "Missing required parameter: " + ex.getParameterName());
        body.put("status", status.value());
        return new ResponseEntity<>(body, headers, status);
    }
 
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            org.springframework.http.HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {
        log.warn("Method not supported: {}", ex.getMethod());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Method Not Allowed");
        body.put("message", ex.getMessage());
        body.put("status", status.value());
        return new ResponseEntity<>(body, headers, status);
    }
 
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, Model model, HttpServletRequest request) {
        log.warn("Access denied for user on URI {}: {}", request.getRequestURI(), ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
        }
        model.addAttribute("errorMessage", "Access Denied: You do not have the required permissions.");
        return "error";
    }
 
    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(IllegalStateException ex, Model model, HttpServletRequest request) {
        log.warn("Invalid workflow operation: {}", ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
 
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
 
    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public Object handleOptimisticLockException(jakarta.persistence.OptimisticLockException ex, Model model, HttpServletRequest request) {
        log.warn("Concurrency conflict: {}", ex.getMessage());
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.CONFLICT, "This record was just modified by another user. Please refresh and try again.");
        }
        model.addAttribute("errorMessage", "This record was just modified by another user.");
        return "error";
    }
 
    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unhandled exception occurred: ", ex);
        if (isRestRequest(request)) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected system error occurred.");
        }
        model.addAttribute("errorMessage", "An unexpected system error occurred. Please contact IT support.");
        return "error";
    }
 
    private boolean isRestRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return (accept != null && accept.contains("application/json")) || uri.contains("/test/workflow/");
    }
 
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }
}
