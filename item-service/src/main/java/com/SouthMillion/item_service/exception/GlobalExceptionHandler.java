package com.SouthMillion.item_service.exception;

import com.SouthMillion.item_service.config.ItemCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemCache.ItemNotFoundException.class)
    public ResponseEntity<?> notFound(ItemCache.ItemNotFoundException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unknown(Exception ex, HttpServletRequest request) {
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String query = request != null ? request.getQueryString() : null;
        log.error("[item-service] Unhandled exception on {} {}{}",
                method,
                uri,
                (query == null || query.isBlank()) ? "" : ("?" + query),
                ex);
        return ResponseEntity.status(500).body(java.util.Map.of("ok", false, "message", "internal error"));
    }
}