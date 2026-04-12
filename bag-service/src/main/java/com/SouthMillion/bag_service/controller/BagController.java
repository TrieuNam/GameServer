package com.SouthMillion.bag_service.controller;

import com.SouthMillion.bag_service.service.BagDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** REST mapping cho Hành trang */
@RestController
@RequestMapping("/api/bag")
@RequiredArgsConstructor
public class BagController {

    private final BagDomainService svc;

    @GetMapping("/{roleId}/items")
    public ResponseEntity<List<BagDTOs.ItemView>> list(@PathVariable String roleId) {
        return ResponseEntity.ok(svc.list(Long.valueOf(roleId)));
    }

    @PostMapping("/{roleId}/items/use")
    public ResponseEntity<Void> use(@PathVariable String roleId,
                                    @Valid @RequestBody BagDTOs.UseItemReq req) {
        svc.use(Long.valueOf(roleId), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/items/sell")
    public ResponseEntity<BagDTOs.SellResult> sell(@PathVariable String roleId,
                                                   @Valid @RequestBody BagDTOs.SellItemReq req) {
        return ResponseEntity.ok(svc.sell(Long.valueOf(roleId), req));
    }

    /** GM/event/quest có thể gọi trực tiếp để grant */
    @PostMapping("/grant")
    public ResponseEntity<List<BagDTOs.ItemView>> grant(@Valid @RequestBody BagDTOs.GrantReq req) {
        String eventId = (req.getEventId() != null && !req.getEventId().isBlank())
                ? req.getEventId()
                : UUID.randomUUID().toString(); // fallback – vẫn idempotent trong phạm vi request này

        List<BagDTOs.ItemView> out = svc.grant(
                req.getUserId(),
                Long.valueOf(req.getRoleId()),
                req.getItems(),
                eventId
        );

        // (tuỳ chọn) gắn luôn eventId vào header để GM dễ tra cứu
        return ResponseEntity.ok()
                .header("X-Event-Id", eventId)
                .body(out);
    }

    /** Lấy tiến trình recycle hiện tại (level + exp) — không thay đổi dữ liệu, dùng cho bootstrap 1686 */
    @GetMapping("/{roleId}/recycle-progress")
    public ResponseEntity<Map<String, Object>> recycleProgress(@PathVariable String roleId) {
        return ResponseEntity.ok(svc.getRecycleProgress(Long.valueOf(roleId)));
    }

    /** Item recycle (物品回收): consume items, award recycle exp, return level+exp */
    @PostMapping("/{roleId}/recycle")
    public ResponseEntity<Map<String, Object>> recycle(
            @PathVariable String roleId,
            @RequestBody List<Integer> itemIds) {
        return ResponseEntity.ok(svc.recycleItems(Long.valueOf(roleId), itemIds));
    }

    @PostMapping("/{roleId}/buy-cmd")
    public ResponseEntity<BagDTOs.SellResult> buyCmd(
            @PathVariable String roleId,
            @Valid @RequestBody BagDTOs.BuyCmdReq req) {
        return ResponseEntity.ok(svc.buyCmd(Long.valueOf(roleId), req));
    }
}
