package com.SouthMillion.starmap_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for config-service to load starmap configuration files
 */
@FeignClient(name = "config-service")
public interface ConfigServiceClient {

    /**
     * Get configuration file from config-service
     *
     * @param path The path to the config file (e.g., "gameworld/starmap/starmap.json")
     * @param ifNoneMatch ETag value for conditional GET (optional)
     * @return ResponseEntity with file contents
     */
    @GetMapping("/api/config/file")
    ResponseEntity<byte[]> getFile(
        @RequestParam("path") String path,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}
