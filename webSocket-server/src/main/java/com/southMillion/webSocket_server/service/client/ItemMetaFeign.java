package com.southMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name="item-service", path="/internal/item")
public interface ItemMetaFeign {
    // ids=1,2,3 -> { "1": {"itemId":1,"pileLimit":99,"isVirtual":0,"normalizedId":1}, ... }
    @GetMapping("/meta")
    Map<String, Map<String,Object>> batchMeta(@RequestParam("ids") String idsCsv);
}