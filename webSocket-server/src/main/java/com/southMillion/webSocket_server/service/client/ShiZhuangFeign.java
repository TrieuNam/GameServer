package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for shizhuang-service
 * Handles fashion/costume system
 */
@FeignClient(name = "shizhuang-service")
public interface ShiZhuangFeign {

    /**
     * Get all clothes owned by a player.
     * Returns list with fields: id, playerId, clothesId, level
     * clothesId matches config entries (e.g. 10001, 20001 ...)
     */
    @GetMapping("/api/shizhuang/list/{roleId}")
    List<Map<String, Object>> getRoleFashions(@PathVariable("roleId") String roleId);

    /**
     * Wear (equip) a fashion/clothes item.
     */
    @PostMapping("/api/shizhuang/wear")
    void wearFashion(@RequestParam("roleId") String roleId,
                     @RequestParam("clothesId") Integer clothesId);

    /**
     * Clear the currently worn state for a fashion item.
     */
    @PostMapping("/api/shizhuang/unwear")
    void unwearFashion(@RequestParam("roleId") String roleId,
                       @RequestParam("clothesId") Integer clothesId);

    /**
     * Get the active appearance snapshot owned by shizhuang-service.
     */
    @GetMapping("/api/shizhuang/appearance/{roleId}")
    Map<String, Object> getCurrentAppearance(@PathVariable("roleId") String roleId);

    /**
     * Level-up a fashion item.
     */
    @PostMapping("/api/shizhuang/levelup")
    void levelUpFashion(@RequestParam("roleId") String roleId,
                        @RequestParam("clothesId") Integer clothesId);

    /**
     * Buy a fashion item from shop.
     */
    @PostMapping("/api/shizhuang/buy")
    void buyFashion(@RequestParam("roleId")      String  roleId,
                    @RequestParam("clothesId")   Integer clothesId,
                    @RequestParam("num")         Integer num,
                    @RequestParam("buyMoney")    Integer buyMoney,
                    @RequestParam("addPayGold")  Integer addPayGold,
                    @RequestParam("buyParam2")   Integer buyParam2);
}
