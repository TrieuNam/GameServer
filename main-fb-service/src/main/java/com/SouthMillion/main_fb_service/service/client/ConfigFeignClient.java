package com.SouthMillion.main_fb_service.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service")
public interface ConfigFeignClient {
    @GetMapping("/api/config/file")
    JsonNode getConfig(@RequestParam("path") String path);
}