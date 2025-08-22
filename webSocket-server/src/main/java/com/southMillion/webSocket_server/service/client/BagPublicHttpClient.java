package com.southMillion.webSocket_server.service.client;


import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bag-service", path = "/api/bag", contextId = "BagPublicFeign")
public interface BagPublicHttpClient {
    @GetMapping("/{roleId}/{bagType}")
    BagDTOs.BagView get(@PathVariable("roleId") String roleId,
                        @PathVariable("bagType") byte bagType);
}