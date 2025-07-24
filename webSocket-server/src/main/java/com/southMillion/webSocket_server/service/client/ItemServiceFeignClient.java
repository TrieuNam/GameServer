package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.item.ItemDto;
import org.SouthMillion.dto.item.ShopDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "item-service")
public interface ItemServiceFeignClient {
    @GetMapping("/api/item/get")
    ItemDto get(@RequestParam("userId") String userId, @RequestParam("itemId") int itemId);

    @GetMapping("/api/item/list")
    List<ItemDto> list(@RequestParam("userId") String userId);

    @PostMapping("/api/item/add")
    ItemDto addOrIncrease(@RequestParam("userId") String userId, @RequestParam("itemId") int itemId, @RequestParam("amount") int amount);

    @PostMapping("/api/item/buy")
    void buy(@RequestParam("userId") String userId, @RequestParam("itemId") int itemId, @RequestParam("amount") int amount);

    @DeleteMapping("/api/item/delete")
    void delete(@RequestParam("userId") String userId, @RequestParam("itemId") int itemId);

    @PostMapping("/gm/add")
    void addItem(@RequestParam String userId, @RequestParam int itemId, @RequestParam int count);

    @PostMapping("/gm/remove")
    void removeItem(@RequestParam String userId, @RequestParam int itemId, @RequestParam int count);

    @GetMapping("/api/shop/limit/{userId}")
    List<ShopDTO> getLimit(@PathVariable("userId") Long userId);

    @PostMapping("/api/shop/buy/{userId}/{index}/{num}")
    void buy(@PathVariable("userId") Long userId, @PathVariable("index") Integer index, @PathVariable("num") Integer num);
}
