package com.SouthMillion.angel_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for bag-service integration
 */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagClient {
    
    /**
     * Use/consume items from player bag
     */
    @PostMapping("/{roleId}/items/use")
    void useItem(
        @PathVariable("roleId") String roleId,
        @RequestBody BagDTOs.UseItemReq request
    );
}
