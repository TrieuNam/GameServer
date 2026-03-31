package com.SouthMillion.shop_service.service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name="item-service", path="/internal/item")
public interface ItemMetaFeign {
    // ids=1,2 -> {"1":{"itemId":1,"isVirtual":1,...}, "2":{...}}
    @GetMapping("/meta")
    Map<String, Map<String,Object>> batchMeta(@RequestParam("ids") String idsCsv);
}