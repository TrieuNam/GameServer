package com.SouthMillion.friend_service.client;

import org.SouthMillion.dto.bag.GrantReq;
import org.SouthMillion.dto.bag.UseItemReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for bag-service integration
 */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagClient {
    
    @PostMapping("/{roleId}/items/use")
    void useItem(
        @PathVariable("roleId") String roleId,
        @RequestBody UseItemReq request
    );
    
    @PostMapping("/grant")
    void grantItems(
        @RequestBody GrantReq request
    );
}
