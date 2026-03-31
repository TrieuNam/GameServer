package com.SouthMillion.equip_service.service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name="bag-service", path="/api/bag", contextId = "BagPublicFeign")
public interface BagPublicFeign {
    @GetMapping("/{roleId}/items")
    List<BagDTOs.ItemView> getBag(@PathVariable("roleId") String roleId);
}