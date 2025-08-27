package com.SouthMillion.shop_service.service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service", path="/config")
public interface ConfigFeign {
    // Ưu tiên by-path để bám đúng key thực tế trong store
    @GetMapping("/by-path")
    ResponseEntity<byte[]> byPath(
            @RequestParam("p") String rel,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestParam(value = "force", required = false, defaultValue = "0") int force
    );
}