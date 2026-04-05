package com.erp.manufacturing.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        log.error("Runtime exception occurred: ", ex);
        model.addAttribute("errorMessage", "An unexpected system error occurred. Please contact IT support.");
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, Model model) {
        log.warn("Invalid workflow operation: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        log.warn("Invalid argument provided: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public String handleOptimisticLockException(jakarta.persistence.OptimisticLockException ex, Model model) {
        log.warn("Concurrency conflict: {}", ex.getMessage());
        model.addAttribute("errorMessage", "This record was just modified by another user. Please refresh and try again.");
        return "error";
    }
}
