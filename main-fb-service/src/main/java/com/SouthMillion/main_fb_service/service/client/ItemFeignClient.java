package com.SouthMillion.main_fb_service.service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "bag-service")
public interface ItemFeignClient {

    @PostMapping("/api/bag/internal/consume")
    ResponseEntity<Void> consumeItem(@RequestBody BagConsumeReq req);

    @PostMapping("/api/bag/internal/add")
    ResponseEntity<List<BagDTOs.ItemView>> addItemsBulk(@RequestBody BagAddItemReq req);
}
