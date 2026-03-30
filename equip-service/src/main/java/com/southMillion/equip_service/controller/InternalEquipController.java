package com.SouthMillion.equip_service.controller;

import com.SouthMillion.equip_service.service.EquipFumoService;
import com.SouthMillion.equip_service.service.EquipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/internal/equip", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalEquipController {

    private final EquipService equipService;
    private final EquipFumoService fumoService;

    // ====== BASIC (để service khác có thể dùng) ======

    @GetMapping("/{roleId}")
    public EquipDTOs.ListResp list(@PathVariable("roleId") Long roleId) {
        return equipService.list(roleId);
    }

    @GetMapping("/snapshot/{roleId}/{equipType}")
    public EquipDTOs.EquipItem snapshot(@PathVariable("roleId") Long roleId,
                                        @PathVariable("equipType") int equipType) {
        return equipService.snapshot(roleId, equipType);
    }

    @DeleteMapping("/snapshot/{roleId}/{equipType}")
    public Map<String, Object> evictSnapshot(@PathVariable("roleId") Long roleId,
                                             @PathVariable("equipType") int equipType) {
        boolean ok = equipService.evictSnapshot(roleId, equipType);
        return Map.of("ok", ok, "roleId", roleId, "equipType", equipType);
    }

    @DeleteMapping("/snapshot/{roleId}")
    public Map<String, Object> evictSnapshotsByRole(@PathVariable("roleId") Long roleId) {
        int removed = equipService.evictSnapshotsByRole(roleId);
        return Map.of("ok", true, "roleId", roleId, "removed", removed);
    }

    @PostMapping(value = "/equip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipDTOs.OkResp equip(@Valid @RequestBody EquipDTOs.EquipReq req) {
        return equipService.equip(req);
    }

    @PostMapping(value = "/unequip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipDTOs.OkResp unequip(@Valid @RequestBody EquipDTOs.UnequipReq req) {
        return equipService.unequip(req);
    }

    // ====== APIs PHỤC VỤ BoxService (giữa microservices) ======

    /** BoxService gọi để mặc món pending; trả về món cũ (replaced) nếu có. */
    @PostMapping(value = "/wear-from-box", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipDTOs.WearFromBoxResp wearFromBox(@RequestBody EquipDTOs.WearFromBoxReq req) {
        return equipService.wearFromBox(req);
    }

    /** BoxService gọi để tính tiền & exp bán equip (không thực thi). */
    @PostMapping(value = "/compute-sell", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> computeSell(@RequestBody Map<String, Object> req) {
        // req: { roleId: string, item: {...} }
        return equipService.computeSell(req);
    }

    /** BoxService gọi để phân giải; trả itemId/num/exp. */
    @PostMapping(value = "/decompose", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> decompose(@RequestBody Map<String, Object> req) {
        // req: { roleId: string, item: {...} }
        return equipService.decompose(req);
    }

    /** Kiểm tra item có phải equip không. */
    @PostMapping(value = "/item-kind", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> itemKind(@RequestBody Map<String, Object> req) {
        // req: { itemId: int }
        return equipService.itemKind(req);
    }

    /** Resolve itemId theo equipType/quality/level. */
    @PostMapping(value = "/resolve-item-id", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> resolveItemId(@RequestBody Map<String, Object> req) {
        // req: { equipType: int, quality: int, level: int }
        return equipService.resolveItemId(req);
    }

    /** Lấy meta (equipType/quality/...) theo itemId. */
    @PostMapping(value = "/item-meta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> itemMeta(@RequestBody Map<String, Object> req) {
        // req: { itemId: int }
        return equipService.itemMeta(req);
    }
}