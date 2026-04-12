package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.exception.FashionBusinessException;
import com.SouthMillion.task_service.exception.FashionErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackageClasses = ShiZhuangController.class)
public class ShiZhuangExceptionHandler {

    @ExceptionHandler(FashionBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleFashionBusiness(FashionBusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage(),
                "itemId", ex.getItemId() == null ? 0 : ex.getItemId()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", FashionErrorCodes.INVALID_ARGUMENT,
                "message", ex.getMessage(),
                "itemId", 0
        ));
    }
}
