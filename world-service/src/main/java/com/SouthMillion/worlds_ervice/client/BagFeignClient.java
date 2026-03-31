package com.SouthMillion.worlds_ervice.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for bag-service integration
 * Used to grant items to player bag when picking up scene items
 */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagFeignClient {

    /**
     * Grant items to player bag (scene item pickup)
     * POST /api/bag/grant
     */
    @PostMapping("/grant")
    void grantItems(@RequestBody BagDTOs.GrantReq request);
}
