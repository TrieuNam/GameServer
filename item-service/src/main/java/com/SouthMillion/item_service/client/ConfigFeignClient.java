package com.SouthMillion.item_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "config-service", url = "http://localhost:8081")
public interface ConfigFeignClient {
    @GetMapping("/api/configs/{name}")
    Object getConfig(@PathVariable("name") String name);
}
