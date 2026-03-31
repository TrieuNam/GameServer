package com.SouthMillion.item_service.service.client;

import com.SouthMillion.item_service.config.FeignConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "config-service-feign",
        url  = "${item.config.base-url}",
        configuration = FeignConfig.class
)
public interface ConfigServiceFeign {

    // Trả thẳng ResponseEntity để lấy ETag từ header
    @GetMapping("/api/config/file")
    ResponseEntity<JsonNode> getFile(
            @RequestParam("path") String path,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}