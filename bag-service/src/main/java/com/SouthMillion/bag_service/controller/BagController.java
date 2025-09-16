package com.SouthMillion.bag_service.controller;

import com.SouthMillion.bag_service.service.BagDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** REST mapping cho Hành trang */
@RestController
@RequestMapping("/api/bag")
@RequiredArgsConstructor
public class BagController {

    private final BagDomainService svc;

    @GetMapping("/{roleId}/items")
    public ResponseEntity<List<BagDTOs.ItemView>> list(@PathVariable String roleId) {
        return ResponseEntity.ok(svc.list(roleId));
    }

    @PostMapping("/{roleId}/items/use")
    public ResponseEntity<Void> use(@PathVariable String roleId,
                                    @Valid @RequestBody BagDTOs.UseItemReq req) {
        svc.use(roleId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/items/sell")
    public ResponseEntity<BagDTOs.SellResult> sell(@PathVariable String roleId,
                                                   @Valid @RequestBody BagDTOs.SellItemReq req) {
        return ResponseEntity.ok(svc.sell(roleId, req));
    }

    /** GM/event/quest có thể gọi trực tiếp để grant */
    @PostMapping("/grant")
    public ResponseEntity<List<BagDTOs.ItemView>> grant(@Valid @RequestBody BagDTOs.GrantReq req) {
        String eventId = (req.getEventId() != null && !req.getEventId().isBlank())
                ? req.getEventId()
                : UUID.randomUUID().toString(); // fallback – vẫn idempotent trong phạm vi request này

        List<BagDTOs.ItemView> out = svc.grant(
                req.getUserId(),
                req.getRoleId(),
                req.getItems(),
                eventId
        );

        // (tuỳ chọn) gắn luôn eventId vào header để GM dễ tra cứu
        return ResponseEntity.ok()
                .header("X-Event-Id", eventId)
                .body(out);
    }
}
