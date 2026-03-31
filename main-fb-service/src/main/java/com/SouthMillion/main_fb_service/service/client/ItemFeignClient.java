package com.SouthMillion.main_fb_service.service.client;

import org.SouthMillion.dto.item.ChangeItemRequestDTO;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "item-service")
public interface ItemFeignClient {

    // ===== Items (theo ItemFacadeController của bạn) =====
    @PostMapping("/api/item/{playerId}/add")
    ResultDTO<Void> addItem(@PathVariable("playerId") String playerId,
                            @RequestBody ChangeItemRequestDTO req,
                            @RequestHeader(value = "X-Request-Id", required = false) String reqId);

    @PostMapping("/api/item/{playerId}/consume")
    ResultDTO<Void> consumeItem(@PathVariable("playerId") String playerId,
                                @RequestBody ChangeItemRequestDTO req,
                                @RequestHeader(value = "X-Request-Id", required = false) String reqId);

    @PostMapping("/api/item/{playerId}/bulk-add")
    ResultDTO<Void> addItemsBulk(@PathVariable("playerId") String playerId,
                                 @RequestBody ChangeItemRequestDTO req,
                                 @RequestHeader(value = "X-Request-Id", required = false) String reqId);

    @PostMapping("/api/item/{playerId}/bulk-consume")
    ResultDTO<Void> consumeItemsBulk(@PathVariable("playerId") String playerId,
                                     @RequestBody ChangeItemRequestDTO req,
                                     @RequestHeader(value = "X-Request-Id", required = false) String reqId);
}