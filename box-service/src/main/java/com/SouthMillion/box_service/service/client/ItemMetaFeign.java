package com.SouthMillion.box_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name="item-service")
public interface ItemMetaFeign {

    @GetMapping("/api/item/meta/batch")
    Map<Integer, Map<String, Object>> batchMeta(@RequestParam("itemId") List<Integer> itemIds);
}