package com.SouthMillion.activity_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.Set;

@FeignClient(name = "config-service", contextId = "activityConfigFeign")
public interface ConfigFeign {

    @GetMapping("/api/config/file")
    @CircuitBreaker(name = "configServiceCircuitBreaker", fallbackMethod = "getFileFallback")
    default ResponseEntity<byte[]> getFile(
            @RequestParam("path")
            String path,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        // Validate config key format server-side before proceeding
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<ConfigKeyWrapper>> violations = validator.validate(new ConfigKeyWrapper(path));
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        // Since this is an interface method (Feign), normally Feign will implement this.
        // Here, for local validation, we can throw an exception or rely on AOP.
        throw new UnsupportedOperationException("Method must be implemented by Feign. Configure AOP for validation.");
    }

    // Fallback method with the same signature plus Throwable param
    default ResponseEntity<byte[]> getFileFallback(String path, String ifNoneMatch, Throwable throwable) {
        // Optionally log the throwable
        return ResponseEntity.status(503).body(null);
    }

    // ConfigKeyWrapper for validation
    class ConfigKeyWrapper {
        @Pattern(regexp = "^[a-zA-Z0-9/_\\-.]+$", message = "Invalid file path")
        private final String path;

        public ConfigKeyWrapper(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}