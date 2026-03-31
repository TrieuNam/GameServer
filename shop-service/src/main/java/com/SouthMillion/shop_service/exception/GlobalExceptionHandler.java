package com.SouthMillion.shop_service.exception;

import org.SouthMillion.dto.shop.ResultDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDTO<Object>> onError(Exception ex) {
        return ResponseEntity.badRequest().body(ResultDTO.fail(ex.getMessage()));
    }
}
