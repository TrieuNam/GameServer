package com.SouthMillion.item_service.exception;

import com.SouthMillion.item_service.config.ItemCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemCache.ItemNotFoundException.class)
    public ResponseEntity<?> notFound(ItemCache.ItemNotFoundException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        if ("/actuator/prometheus".equals(uri)) {
            log.debug("[item-service] Metrics endpoint not available on {} {}", method, uri);
        } else {
            log.warn("[item-service] No resource on {} {}", method, uri);
        }
        return ResponseEntity.status(404).body(java.util.Map.of("ok", false, "message", "not found"));
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