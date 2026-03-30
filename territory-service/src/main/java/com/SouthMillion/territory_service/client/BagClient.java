package com.SouthMillion.territory_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

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
    
    /**
     * Grant items to player bag
     */
    @PostMapping("/grant")
    List<BagDTOs.ItemView> grantItems(
        @RequestBody BagDTOs.GrantReq request
    );
}
