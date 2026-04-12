package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.ShiZhuangService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.PlayerClothesDTO;
import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shizhuang")
@RequiredArgsConstructor
public class ShiZhuangController {
    private final ShiZhuangService service;

    @GetMapping("/get")
    public ResponseEntity<ShiZhuangDto> get(@RequestParam String userId, @RequestParam int id) {
        ShiZhuangDto res = service.get(userId, id);
        if (res == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ShiZhuangDto>> list(@RequestParam String userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<ShiZhuangDto> add(@RequestBody ShiZhuangDto dto) {
        return ResponseEntity.ok(service.addOrUpdate(dto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String userId, @RequestParam int id) {
        boolean ok = service.delete(userId, id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // API mua thời trang (shop)
    @PostMapping("/buy")
    public ResponseEntity<?> buyClothes(@RequestParam("roleId") String roleId,
                                        @RequestParam("clothesId") Integer clothesId,
                                        @RequestParam("num") Integer num,
                                        @RequestParam("buyMoney") Integer buyMoney,
                                        @RequestParam("addPayGold") Integer addPayGold,
                                        @RequestParam("buyParam2") Integer buyParam2) {
        service.buyClothes(roleId, clothesId, num, buyMoney, addPayGold, buyParam2);
        return ResponseEntity.ok().build();
    }

    // API mặc thời trang
    @PostMapping("/wear")
    public ResponseEntity<?> wear(@RequestParam String roleId,
                                  @RequestParam Integer clothesId) {
        service.wearClothes(roleId, clothesId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unwear")
    public ResponseEntity<?> unwear(@RequestParam String roleId,
                                    @RequestParam Integer clothesId) {
        service.unwearClothes(roleId, clothesId);
        return ResponseEntity.ok().build();
    }

    // API nâng cấp thời trang
    @PostMapping("/levelup")
    public ResponseEntity<?> levelUp(@RequestParam String roleId,
                                     @RequestParam Integer clothesId,
                                     @RequestParam(value = "consumeMode", required = false, defaultValue = "0") Integer consumeMode) {
        service.levelUpClothes(roleId, clothesId, consumeMode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appearance/{roleId}")
    public ResponseEntity<Map<String, Integer>> getCurrentAppearance(@PathVariable String roleId) {
        return ResponseEntity.ok(service.getCurrentAppearance(roleId));
    }

    // API lấy danh sách thời trang sở hữu
    @GetMapping("/list/{roleId}")
    public List<PlayerClothesDTO> getClothes(@PathVariable String roleId) {
        return service.getClothes(roleId);
    }

}