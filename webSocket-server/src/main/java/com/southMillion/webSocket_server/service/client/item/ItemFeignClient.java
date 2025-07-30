package com.southMillion.webSocket_server.service.client.item;

import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.dto.item.Knapsack.ItemRetrieveConfigDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "item-service", contextId = "itemFeignClient")
public interface ItemFeignClient {
    @GetMapping("/api/item/{userId}")
    List<ItemDTO> getItems(@PathVariable("userId") String userId);

    @PostMapping("/api/item/{userId}/add")
    void addItem(@PathVariable("userId") String userId, @RequestParam int itemId, @RequestParam int count);

    @PostMapping("/api/item/{userId}/consume")
    boolean consume(@PathVariable("userId") String userId, @RequestParam int itemId, @RequestParam int count);

    @GetMapping("/api/item-config/{itemId}")
    Object getItemConfig(@PathVariable("itemId") int itemId);

    @GetMapping("/item-retrieve")
    ItemRetrieveConfigDTO getItemRetrieveConfig();

    @GetMapping("/api/item/{userId}/notenough/{itemId}")
    Boolean isNotEnough(@PathVariable("userId") String userId,
                        @PathVariable("itemId") int itemId,
                        @RequestParam("count") int count);

    @GetMapping("/api/item/{userId}/single")
    ItemDTO getSingle(@PathVariable("userId") String userId, @RequestParam int itemId);
}