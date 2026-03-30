package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for block-service (Building Block / 積木 system)
 * Handles building block game feature operations.
 */
@FeignClient(name = "block-service", path = "/api/block")
public interface BlockFeign {

    /** 嵌入: embed a block into a map position */
    @PostMapping("/inlay")
    Map<String, Object> inlay(@RequestBody Map<String, Object> req);

    /** 拆除: remove a block from a map position */
    @PostMapping("/remove")
    Map<String, Object> remove(@RequestBody Map<String, Object> req);

    /** 合成: compose multiple blocks into one */
    @PostMapping("/compose")
    Map<String, Object> compose(@RequestBody Map<String, Object> req);

    /** 激活: activate a block sequence level */
    @PostMapping("/activate")
    Map<String, Object> activate(@RequestBody Map<String, Object> req);

    /** 装备模型: wear/equip a map model */
    @PostMapping("/wear")
    Map<String, Object> wear(@RequestBody Map<String, Object> req);

    /** Get full block info for a role (initial load) */
    @GetMapping("/info")
    Map<String, Object> getInfo(@RequestParam("roleId") String roleId);
}
