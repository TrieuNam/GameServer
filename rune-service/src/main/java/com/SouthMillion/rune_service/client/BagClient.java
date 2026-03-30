package com.SouthMillion.rune_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagClient {
    
    @PostMapping("/{roleId}/items/use")
    void useItem(
        @PathVariable String roleId,
        @RequestBody BagDTOs.UseItemReq request
    );
}
