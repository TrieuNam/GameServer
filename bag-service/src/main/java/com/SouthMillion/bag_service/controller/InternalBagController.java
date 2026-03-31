package com.SouthMillion.bag_service.controller;

import com.SouthMillion.bag_service.service.BagDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bag/internal")
@RequiredArgsConstructor
public class InternalBagController {

    private final BagDomainService svc;

    /** Internal: add items to bag (called by equip, drop, gift, shop services) */
    @PostMapping("/add")
    public ResponseEntity<List<BagDTOs.ItemView>> add(@Valid @RequestBody BagAddItemReq req) {
        Long roleId = req.getRoleId();
        String userId = req.getUserId() != null ? String.valueOf(req.getUserId()) : String.valueOf(roleId);
        String eventId = (req.getIdemKey() != null && !req.getIdemKey().isBlank())
                ? req.getIdemKey() : UUID.randomUUID().toString();

        List<BagDTOs.GrantItem> items = req.getItems().stream()
                .map(i -> BagDTOs.GrantItem.builder()
                        .itemId(i.getItemId())
                        .num(i.getAmount())
                        .quality(i.getQuality())
                        .bind(i.getBound() != null && i.getBound() ? 1 : 0)
                        .bagType(i.getBagType())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(svc.grant(userId, roleId, items, eventId));
    }

    /** Internal: list items for a role (optionally filtered by bagType). */
    @GetMapping("/items/{roleId}")
    public ResponseEntity<List<BagDTOs.ItemView>> listItems(
            @PathVariable String roleId,
            @RequestParam(required = false) Integer bagType) {
        List<BagDTOs.ItemView> all = svc.list(Long.valueOf(roleId));
        if (bagType != null) {
            int bt = bagType;
            all = all.stream().filter(i -> i.getBagType() != null && i.getBagType() == bt).toList();
        }
        return ResponseEntity.ok(all);
    }

    /** Internal: consume items from bag (called by equip, gift, shop services) */
    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@Valid @RequestBody BagConsumeReq req) {
        Long roleId = req.getRoleId();
        if (req.getCosts() != null && !req.getCosts().isEmpty()) {
            for (BagConsumeReq.Cost cost : req.getCosts()) {
                BagDTOs.UseItemReq useReq = BagDTOs.UseItemReq.builder()
                        .itemId(cost.getItemId())
                        .num(cost.getAmount())
                        .build();
                svc.use(roleId, useReq);
            }
        } else {
            BagDTOs.UseItemReq useReq = BagDTOs.UseItemReq.builder()
                    .itemId(req.getItemId())
                    .num(req.getAmount())
                    .build();
            svc.use(roleId, useReq);
        }
        return ResponseEntity.noContent().build();
    }
}

