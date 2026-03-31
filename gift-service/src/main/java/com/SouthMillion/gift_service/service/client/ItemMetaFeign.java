package com.SouthMillion.gift_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/** item-service: trả { "40400": {"itemId":40400,"pileLimit":99,"isVirtual":0,"normalizedId":40400}, ... } */
@FeignClient(name = "item-service", path = "/internal/item")
public interface ItemMetaFeign {
    @GetMapping("/meta")
    Map<String, Map<String, Object>> batchMeta(@RequestParam("ids") String idsCsv);
}