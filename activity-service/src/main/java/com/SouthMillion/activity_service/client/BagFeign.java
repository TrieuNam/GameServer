package com.SouthMillion.activity_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "bag-service", path = "/api/bag", contextId = "activityBagFeign")
public interface BagFeign {

    @PostMapping("/grant")
    List<BagDTOs.ItemView> grantItems(@RequestBody BagDTOs.GrantReq request);
}