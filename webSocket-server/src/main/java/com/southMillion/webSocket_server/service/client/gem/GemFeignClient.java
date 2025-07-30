package com.southMillion.webSocket_server.service.client.gem;

import org.SouthMillion.dto.item.gem.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "item-service", contextId = "gemFeignClient")
public interface GemFeignClient {
    @GetMapping("/api/gem/all")
    List<GemstoneDTO> getAllGems();

    @GetMapping("/api/gem/{id}")
    GemstoneDTO getGem(@PathVariable("id") Integer id);

    @GetMapping("/api/gem/drawing/all")
    List<GemstoneDrawingDTO> getAllDrawings();

    @GetMapping("/api/gem/drawing/{id}")
    GemDrawingDTO getDrawing(@PathVariable("id") Integer id);

    @GetMapping("/api/gem/compound/all")
    List<GemCompoundDTO> getAllGemCompounds();

    @GetMapping("/api/gem/drawing_up/all")
    List<GemDrawingUpDTO> getAllDrawingUp();

    @PostMapping("/api/gem/buy")
    void buyGem(@RequestParam("userId") Long userId, @RequestBody List<Integer> itemIds);

    @GetMapping("/drawing/user/{userId}")
    List<GemDrawingDTO> getAllDrawingsByUser(@PathVariable("userId") Long userId);

    @GetMapping("/api/gem/drawing/user/{userId}/{drawingId}")
    GemDrawingDTO getDrawingByUser(@PathVariable("userId") Long userId, @PathVariable("drawingId") Long drawingId);

    @PostMapping("/api/gem/upgrade/onekey")
    void oneKeyUpgradeGem(@RequestParam("userId") Long userId, @RequestBody List<Integer> itemIds);
}