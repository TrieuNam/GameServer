package com.SouthMillion.mount_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for config-service integration
 * Loads mount and harness configuration files
 */
@FeignClient(name = "config-service")
public interface ConfigServiceClient {

    /**
     * Get configuration file from config-service
     * Supports ETag-based caching with If-None-Match header
     *
     * @param path Config file path (e.g., "gameworld/mount/harness.json")
     * @param ifNoneMatch ETag from previous request for conditional GET
     * @return ResponseEntity with file content or 304 Not Modified
     */
    @GetMapping("/api/config/file")
    ResponseEntity<byte[]> getFile(
        @RequestParam("path") String path,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}
