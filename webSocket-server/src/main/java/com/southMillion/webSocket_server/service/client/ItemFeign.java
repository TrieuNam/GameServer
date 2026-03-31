package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for item-service public API
 */
@FeignClient(name = "item-service", contextId = "ItemFeign")
public interface ItemFeign {

    /**
     * Recycle items to get rewards
     */
    @PostMapping("/api/item/{roleId}/recycle")
    Map<String, Object> recycleItems(
            @PathVariable("roleId") Long roleId,
            @RequestBody List<Integer> itemIds);
}
