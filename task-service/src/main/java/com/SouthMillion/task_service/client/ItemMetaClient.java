package com.SouthMillion.task_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "item-service")
public interface ItemMetaClient {

    @GetMapping("/api/item/meta")
    Map<String, Object> meta(@RequestParam("itemId") int itemId);
}
