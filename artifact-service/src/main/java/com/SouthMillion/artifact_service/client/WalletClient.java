package com.SouthMillion.artifact_service.client;

import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for wallet-service
 * Handle artifact currency operations
 */
@FeignClient(name = "wallet-service")
public interface WalletClient {
    
    /**
     * Add currency to player wallet
     */
    @PostMapping("/api/wallet/{roleId}/add")
    ResponseEntity<String> addCurrency(@PathVariable("roleId") String roleId,
                                       @RequestBody WalletDTOs.BatchReq request);
    
    /**
     * Consume currency from player wallet
     */
    @PostMapping("/api/wallet/{roleId}/consume")
    ResponseEntity<String> consumeCurrency(@PathVariable("roleId") String roleId,
                                           @RequestBody WalletDTOs.BatchReq request);
}
