package com.SouthMillion.bag_service.controller;

import com.SouthMillion.bag_service.service.BagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bag")
@RequiredArgsConstructor
public class BagController {
    private final BagService svc;

    @GetMapping("/{roleId}/{bagType}")
    public BagDTOs.BagView get(@PathVariable("roleId") String roleId,
                               @PathVariable("bagType") byte bagType) {
        return svc.get(roleId, bagType);
    }

    @PostMapping("/sort")
    public BagDTOs.BagView sort(@Valid @RequestBody BagDTOs.SortReq req) {
        return svc.sortCompact(req);
    }

    @PostMapping("/expand")
    public BagDTOs.OkResp expand(@Valid @RequestBody BagDTOs.ExpandReq req) {
        return svc.expand(req);
    }
}