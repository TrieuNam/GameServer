package com.SouthMillion.activity_service.client;

import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "box-service", path = "/api/box", contextId = "activityBoxFeign")
public interface BoxFeign {

    @GetMapping("/info")
    BoxDTOs.InfoResp info(@RequestParam("roleId") Long roleId);
}