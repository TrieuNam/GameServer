package com.SouthMillion.role_service.service.client;

import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "wallet-service", path = "/internal/wallet", contextId = "RoleSkillWalletFeign")
public interface WalletFeign {

    @PostMapping("/batch-cost")
    ResultDTO<WalletDTOs.MutateResp> batchCost(@RequestBody WalletDTOs.BatchReq req);

    @PostMapping("/batch-add")
    ResultDTO<WalletDTOs.MutateResp> batchAdd(@RequestBody WalletDTOs.BatchReq req);

    /** Check if the player has at least {@code amount} of the given currency type. */
    @GetMapping("/{roleId}/has-enough")
    Boolean hasEnough(
            @PathVariable("roleId") String roleId,
            @RequestParam("currencyType") String currencyType,
            @RequestParam("amount") Long amount
    );

    /** Deduct currency. Body: { "roleId", "currencyType", "amount" } */
    @PostMapping("/deduct")
    Map<String, Object> deductCurrency(@RequestBody Map<String, Object> req);
}
