package com.SouthMillion.wallet_service.controller;

import com.SouthMillion.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/wallet")
@RequiredArgsConstructor
public class InternalWalletController {
    private final WalletService svc;

    @PostMapping("/batch-add")
    public ResultDTO<WalletDTOs.MutateResp> batchAdd(@Valid @RequestBody WalletDTOs.BatchReq req) {
        return ResultDTO.ok(svc.batchAdd(req));
    }

    @PostMapping("/batch-cost")
    public ResultDTO<WalletDTOs.MutateResp> batchCost(@Valid @RequestBody WalletDTOs.BatchReq req) {
        try {
            return ResultDTO.ok(svc.batchCost(req));
        } catch (IllegalStateException e) {
            return ResultDTO.err(1001, e.getMessage()); // INSUFFICIENT_FUNDS:xxx
        }
    }

    @GetMapping("/{roleId}")
    public ResultDTO<WalletDTOs.BalancesResp> get(
            @PathVariable String roleId,
            @RequestParam("itemIds") List<Long> itemIds) {
        return ResultDTO.ok(svc.get(roleId, itemIds));
    }
}