package com.SouthMillion.task_service.service.client;

import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.dto.item.Knapsack.ItemRetrieveConfigDTO;
import org.SouthMillion.dto.item.Knapsack.PlayerItemDTO;
import org.SouthMillion.dto.item.ManualInfoDTO;
import org.SouthMillion.dto.item.UserProgressDTO;
import org.SouthMillion.dto.item.knights.RewardItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "item-service", contextId = "itemshizhuangFeignClient")
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


    @PostMapping("/api/knapsack/use")
    void useItem(@RequestParam("roleId") Long roleId, @RequestParam("itemId") Long itemId, @RequestParam("count") int count);

    @PostMapping("/api/knapsack/sell")
    Integer sellItem(@RequestParam("roleId") Long roleId, @RequestParam("itemId") Long itemId, @RequestParam("count") int count);

    @GetMapping("/api/knapsack/bag/{roleId}")
    List<PlayerItemDTO> getBag(@PathVariable("playerId") String playerId);

    @PostMapping("/api/item/buy")
    void buyItem(@RequestParam("playerId") String playerId,
                 @RequestParam("itemId") Integer itemId,
                 @RequestParam("num") Integer num,
                 @RequestParam("buyMoney") Integer buyMoney,
                 @RequestParam("addPayGold") Integer addPayGold,
                 @RequestParam("buyParam2") Integer buyParam2);


    @GetMapping("/api/manual/{userId}")
    ManualInfoDTO getManualInfo(@PathVariable("userId") Long userId);

    @PostMapping("/api/manual/{userId}/levelup")
    ManualInfoDTO levelUp(@PathVariable("userId") Long userId, @RequestBody UserProgressDTO userProgress);

    @PostMapping("/api/manual/{userId}/fetch-reward")
    List<RewardItemDTO> fetchReward(@PathVariable("userId") Long userId, @RequestParam("level") int level);


}