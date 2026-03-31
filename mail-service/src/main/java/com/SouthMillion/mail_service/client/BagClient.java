package com.SouthMillion.mail_service.client;

import org.SouthMillion.dto.bag.GrantReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for bag-service integration
 */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagClient {
    
    @PostMapping("/grant")
    void grantItems(
        @RequestBody GrantReq request
    );
}
