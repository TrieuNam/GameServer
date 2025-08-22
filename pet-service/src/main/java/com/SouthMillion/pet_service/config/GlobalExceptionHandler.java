package com.SouthMillion.pet_service.config;

import lombok.Data;
import org.SouthMillion.exception.BizException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<?> handleBizException(BizException e) {
        return ResponseEntity
                .status(200) // hoặc 400, tuỳ convention
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @Data
    public static class ErrorResponse {
        private final int code;
        private final String message;
    }
}