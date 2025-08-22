package com.SouthMillion.shop_service.service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service")
public interface ConfigFeign {
    // Ưu tiên by-path để bám đúng key thực tế trong store
    @GetMapping("/config/by-path")
    ResponseEntity<byte[]> byPath(@RequestParam("p") String path,
                                  @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);
}