package com.SouthMillion.shizhuang_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for Wallet Service
 * Handles gold and diamond transactions for shizhuang operations
 */
@FeignClient(name = "wallet-service", path = "/internal/wallet")
public interface WalletFeignClient {
    
    /**
     * Add currency (gold or diamond)
     */
    @PostMapping("/add")
    Map<String, Object> addCurrency(@RequestBody Map<String, Object> req);
    
    /**
     * Deduct currency
     */
    @PostMapping("/deduct")
    Map<String, Object> deductCurrency(@RequestBody Map<String, Object> req);
    
    /**
     * Get wallet balance
     */
    @GetMapping("/{roleId}")
    Map<String, Object> getBalance(@PathVariable("roleId") String roleId);
    
    /**
     * Check if has enough currency
     */
    @GetMapping("/{roleId}/has-enough")
    Boolean hasEnough(
            @PathVariable("roleId") String roleId,
            @RequestParam("currencyType") String currencyType,
            @RequestParam("amount") Long amount
    );
}
