package com.SouthMillion.box_service.service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="bag-service", path="/internal/bag")
public interface BagFeign {
    @PostMapping("/add")    BagDTOs.AddItemResp add(@RequestBody BagDTOs.AddItemReq req);
    @PostMapping("/consume")BagDTOs.OkResp      consume(@RequestBody BagDTOs.ConsumeReq req);
}