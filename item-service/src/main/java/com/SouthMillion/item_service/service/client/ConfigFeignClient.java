package com.SouthMillion.item_service.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "config-service")
public interface ConfigFeignClient {
    @GetMapping("/api/config/{fileName}")
    JsonNode getConfigFile(@PathVariable("fileName") String fileName);
}