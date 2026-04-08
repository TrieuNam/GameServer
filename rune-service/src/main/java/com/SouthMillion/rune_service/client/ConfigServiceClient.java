package com.SouthMillion.rune_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for config-service
 * Loads rune configuration files from config-service
 */
@FeignClient(name = "config-service")
public interface ConfigServiceClient {

    /**
     * Get configuration file from config-service
     * Supports ETag-based conditional GET for caching
     *
     * @param path Path to config file (e.g., "gameworld/rune/rune.json")
     * @param ifNoneMatch ETag from previous request for conditional GET
     * @return Config file content as byte array, or 304 Not Modified
     */
    @GetMapping("/api/config/file")
    ResponseEntity<byte[]> getFile(
        @RequestParam("path") String path,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}
