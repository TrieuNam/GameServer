package com.SouthMillion.arenaservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign Client for Wallet Service
 * Used by arena-service for claiming ranking rewards and buying extra challenges
 */
@FeignClient(name = "wallet-service", path = "/internal/wallet")
public interface WalletFeignClient {

    /**
     * Add currency (gold) to player wallet — for reward claiming
     */
    @PostMapping("/add")
    Map<String, Object> addCurrency(@RequestBody Map<String, Object> req);

    /**
     * Deduct currency (diamonds/gold) from player wallet — for buying extra challenges
     */
    @PostMapping("/deduct")
    Map<String, Object> deductCurrency(@RequestBody Map<String, Object> req);
}
