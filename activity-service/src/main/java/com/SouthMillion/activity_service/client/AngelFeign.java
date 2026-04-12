package com.SouthMillion.activity_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "angel-service", path = "/api/angel")
public interface AngelFeign {

    @GetMapping("/{roleId}")
    Map<String, Object> getAngelData(@PathVariable("roleId") Long roleId);
}
