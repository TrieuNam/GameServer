package com.SouthMillion.gift_service.service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagAddItemResp;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagOkResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="bag-service", path="/api/bag/internal")
public interface BagInternalFeign {
    @PostMapping("/add")
    BagAddItemResp add(@RequestBody BagAddItemReq req);

    @PostMapping("/consume")
    BagOkResp consume(@RequestBody BagConsumeReq req);
}
