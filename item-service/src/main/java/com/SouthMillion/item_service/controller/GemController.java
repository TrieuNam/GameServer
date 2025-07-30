package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.GemService;
import org.SouthMillion.dto.item.gem.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/gem")
public class GemController {
    @Autowired
    private GemService gemService;

    // Lấy toàn bộ gem
    @GetMapping("/all")
    public List<GemstoneDTO> getAllGems() {
        return gemService.getAllGems();
    }

    // Lấy gem theo id
    @GetMapping("/{id}")
    public GemstoneDTO getGem(@PathVariable Integer id) {
        GemstoneDTO gem = gemService.getGemById(id);
        if (gem == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gem not found");
        return gem;
    }

    // Lấy toàn bộ drawing
    @GetMapping("/drawing/all")
    public List<GemstoneDrawingDTO> getAllDrawings() {
        return gemService.getAllDrawings();
    }

    // Lấy drawing theo id
    @GetMapping("/drawing/{id}")
    public GemstoneDrawingDTO getDrawing(@PathVariable Integer id) {
        GemstoneDrawingDTO draw = gemService.getDrawingById(id);
        if (draw == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Drawing not found");
        return draw;
    }

    // Lấy toàn bộ công thức ghép gem
    @GetMapping("/compound/all")
    public List<GemCompoundDTO> getAllGemCompounds() {
        return gemService.getAllGemCompounds();
    }

    // Lấy toàn bộ config tăng cấp bản vẽ
    @GetMapping("/drawing_up/all")
    public List<GemDrawingUpDTO> getAllDrawingUp() {
        return gemService.getAllDrawingUp();
    }

    // API mua gem
    @PostMapping("/buy")
    public ResponseEntity<?> buyGem(@RequestParam Long userId, @RequestBody List<Integer> itemIds) {
        try {
            gemService.buyGem(userId, itemIds);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/drawing/user/{userId}")
    public List<GemDrawingDTO> getAllDrawingsByUser(@PathVariable("userId") Long userId) {
        return gemService.getAllDrawingsByUser(userId);
    }

    @GetMapping("/drawing/user/{userId}/{drawingId}")
    public GemDrawingDTO getDrawingByUser(@PathVariable("userId") Long userId,
                                          @PathVariable("drawingId") Long drawingId) {
        return gemService.getDrawingByUser(userId, drawingId);
    }

    @PostMapping("/upgrade/onekey")
    public void oneKeyUpgradeGem(@RequestParam("userId") Long userId, @RequestBody List<Integer> itemIds) {
        gemService.oneKeyUpgradeGem(userId, itemIds);
    }

}
