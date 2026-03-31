package com.SouthMillion.gm.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "wallet-service")
public interface WalletFeignClient {
    
    @PostMapping("/api/wallet/{playerId}/add")
    Map<String, Object> addCurrency(@PathVariable String playerId, @RequestBody Map<String, Object> request);
    
    @PostMapping("/api/wallet/{playerId}/deduct")
    Map<String, Object> deductCurrency(@PathVariable String playerId, @RequestBody Map<String, Object> request);
    
    @GetMapping("/api/wallet/{playerId}")
    Map<String, Object> getWallet(@PathVariable String playerId);
}
