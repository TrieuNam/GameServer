package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/** Khớp controller ở bag-service */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagFeign {

    @GetMapping("/{roleId}/items")
    List<BagDTOs.ItemView> list(@PathVariable("roleId") String roleId);

    @PostMapping("/{roleId}/items/use")
    void use(@PathVariable("roleId") String roleId,
             @RequestBody BagDTOs.UseItemReq req);

    @PostMapping("/{roleId}/items/sell")
    BagDTOs.SellResult sell(@PathVariable("roleId") String roleId,
                            @RequestBody BagDTOs.SellItemReq req);

    @PostMapping("/grant")
    List<BagDTOs.ItemView> grant(@RequestBody BagDTOs.GrantReq req);

    /** Lấy tiến trình recycle (level + exp) — dùng cho bootstrap push 1686 */
    @GetMapping("/{roleId}/recycle-progress")
    Map<String, Object> getRecycleProgress(@PathVariable("roleId") String roleId);

    /** Item recycle (物品回收) — consume items, return {level, exp} */
    @PostMapping("/{roleId}/recycle")
    Map<String, Object> recycleItems(@PathVariable("roleId") String roleId,
                                     @RequestBody List<Integer> itemIds);

    @PostMapping("/{roleId}/buy-cmd")
    BagDTOs.SellResult buyCmd(@PathVariable("roleId") String roleId,
                              @RequestBody BagDTOs.BuyCmdReq req);
}