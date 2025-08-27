package com.SouthMillion.box_service.service.client;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "equip-service",
        path = "/internal/equip"
)
public interface EquipFeign {

    // ===== APIs BoxService cần =====

    /** Mặc món pending từ Box; trả về { "replaced": { ... } } nếu có món cũ. */
    @PostMapping(value = "/wear-from-box", consumes = "application/json", produces = "application/json")
    Map<String, Object> wearFromBox(@RequestBody Map<String, Object> req);

    /** Tính coin/exp khi bán equip; trả về { "coin": long, "exp": long }. */
    @PostMapping(value = "/compute-sell", consumes = "application/json", produces = "application/json")
    Map<String, Object> computeSell(@RequestBody Map<String, Object> req);

    /** Tính kết quả phân giải; trả về { "itemId": int, "num": long, "exp": long }. */
    @PostMapping(value = "/decompose", consumes = "application/json", produces = "application/json")
    Map<String, Object> decompose(@RequestBody Map<String, Object> req);

    /** Kiểm tra item có phải equip: trả về { "equip": true/false }. */
    @PostMapping(value = "/item-kind", consumes = "application/json", produces = "application/json")
    Map<String, Object> itemKind(@RequestBody Map<String, Object> req);

    /** Resolve itemId theo (equipType, quality, level); có thể trả rỗng nếu không tìm được. */
    @PostMapping(value = "/resolve-item-id", consumes = "application/json", produces = "application/json")
    Map<String, Object> resolveItemId(@RequestBody Map<String, Object> req);

    /** Lấy meta rút gọn theo itemId: có thể trả { "equipType", "quality", "level" }. */
    @PostMapping(value = "/item-meta", consumes = "application/json", produces = "application/json")
    Map<String, Object> itemMeta(@RequestBody Map<String, Object> req);

    // ===== (tuỳ chọn) basic internal dùng lại service =====

    /** Danh sách equip theo slot cho 1 role. */
    @GetMapping(value = "/{roleId}", produces = "application/json")
    EquipDTOs.ListResp list(@PathVariable("roleId") String roleId);

    /** Mặc từ túi (public API mirror – dùng nội bộ nếu cần). */
    @PostMapping(value = "/equip", consumes = "application/json", produces = "application/json")
    EquipDTOs.OkResp equip(@RequestBody EquipDTOs.EquipReq req);

    /** Tháo trang bị (public API mirror – dùng nội bộ nếu cần). */
    @PostMapping(value = "/unequip", consumes = "application/json", produces = "application/json")
    EquipDTOs.OkResp unequip(@RequestBody EquipDTOs.UnequipReq req);
}