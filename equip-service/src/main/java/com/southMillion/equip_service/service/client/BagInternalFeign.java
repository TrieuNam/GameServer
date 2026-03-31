package com.SouthMillion.equip_service.service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="bag-service", path="/api/bag/internal", contextId = "BagInternalFeign")
public interface BagInternalFeign {
    @PostMapping("/add")
    ResponseEntity<List<BagDTOs.ItemView>> add(@RequestBody BagAddItemReq req);

    @PostMapping("/consume")
    ResponseEntity<Void> consume(@RequestBody BagConsumeReq req);
}
