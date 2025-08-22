package com.SouthMillion.gameworld_service.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service")
public interface ConfigFeignClient {
    // Trả JSON theo fileName (không cần .json), phía config-service map đúng thư mục
    @GetMapping("/api/config/{fileName}")
    JsonNode getConfigFile(@PathVariable("fileName") String fileName);

    // Option: cho phép chỉ định path đầy đủ nếu bạn muốn tách namespace
    @GetMapping("/api/config/path")
    JsonNode getConfigByPath(@RequestParam("path") String path);
}
