package com.SouthMillion.task_service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for bag-service integration
 */
@FeignClient(name = "bag-service", path = "/api/bag/internal")
public interface BagClient {

    @PostMapping("/add")
    List<BagDTOs.ItemView> grantItems(@RequestBody BagAddItemReq req);
}
