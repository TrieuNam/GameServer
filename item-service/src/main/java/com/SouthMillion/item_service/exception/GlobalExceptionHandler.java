package com.SouthMillion.item_service.exception;

import com.SouthMillion.item_service.config.ItemCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemCache.ItemNotFoundException.class)
    public ResponseEntity<?> notFound(ItemCache.ItemNotFoundException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unknown(Exception ex) {
        return ResponseEntity.status(500).body(java.util.Map.of("ok", false, "message", "internal error"));
    }
}