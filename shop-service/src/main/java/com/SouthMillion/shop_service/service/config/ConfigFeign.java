package com.SouthMillion.shop_service.service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service")
public interface ConfigFeign {
    @GetMapping("/api/config/file")
    ResponseEntity<byte[]> getFile(
            @RequestParam("path") String path,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}