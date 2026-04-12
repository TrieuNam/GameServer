package com.SouthMillion.role_service.service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "bag-service", path = "/api/bag/internal", contextId = "RoleBagFeign")
public interface BagFeign {

    @PostMapping("/add")
    List<BagDTOs.ItemView> add(@RequestBody BagAddItemReq req);
}
