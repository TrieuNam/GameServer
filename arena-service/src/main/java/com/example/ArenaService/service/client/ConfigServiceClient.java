package com.example.ArenaService.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "config-service", url = "${config-service.url}")
public interface ConfigServiceClient {

    @GetMapping("/api/config/arena")
    JsonNode getArenaConfigJson();

    @GetMapping("/api/config/df_arena")
    JsonNode getDfArenaConfigJson();
}
