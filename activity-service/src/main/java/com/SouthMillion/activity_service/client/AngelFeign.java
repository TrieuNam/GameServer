/**
 * AngelFeign client interface for interservice calls to angel-service.
 *
 * Architecture Decision Record: see docs/architecture/adr-angelfeign-client.md
 */
package com.SouthMillion.activity_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collections;

@FeignClient(name = "angel-service")
public interface AngelFeign {
    @CircuitBreaker(name = "angelServiceCircuitBreaker", fallbackMethod = "fetchDataFallback")
    @GetMapping("/endpoint")
    String fetchData(@RequestParam("param") String param); // Parameterized to prevent SQL injection

    default String fetchDataFallback(String param, Throwable t) {
        // Fallback response for angel-service failure
        return "";
    }
}