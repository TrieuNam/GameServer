package com.SouthMillion.box_service.service.client;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.bag.BagOkResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="bag-service", path="/api/bag/internal")
public interface BagFeign {
    @PostMapping("/add")    List<BagDTOs.ItemView>   add(@RequestBody BagAddItemReq req);
    @PostMapping("/consume")BagOkResp                consume(@RequestBody BagConsumeReq req);
    @GetMapping("/items/{roleId}") List<BagDTOs.ItemView> listItems(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "bagType", required = false) Integer bagType);
}