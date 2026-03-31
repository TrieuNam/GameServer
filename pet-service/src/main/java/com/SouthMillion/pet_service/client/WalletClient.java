package com.SouthMillion.pet_service.client;

import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for wallet-service integration
 */
@FeignClient(name = "wallet-service", path = "/api/wallet")
public interface WalletClient {
    
    @PostMapping("/{playerId}/consume")
    WalletDTOs.MutateResp consumeCurrency(
        @PathVariable("playerId") String playerId,
        @RequestBody WalletDTOs.BatchReq request
    );
}
