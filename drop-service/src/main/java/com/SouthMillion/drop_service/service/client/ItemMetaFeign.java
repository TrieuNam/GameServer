package com.SouthMillion.drop_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name="item-service", path="/internal/item")
public interface ItemMetaFeign {
    @GetMapping("/meta")
    Map<String, Map<String,Object>> batchMeta(@RequestParam("ids") String idsCsv);
}