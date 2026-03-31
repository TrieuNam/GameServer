package com.SouthMillion.rune_service.client;

import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service", path = "/api/wallet")
public interface WalletClient {
    
    @PostMapping("/{playerId}/consume")
    WalletDTOs.MutateResp consumeCurrency(
        @PathVariable String playerId,
        @RequestBody WalletDTOs.BatchReq request
    );
    
    @PostMapping("/{playerId}/add")
    WalletDTOs.MutateResp addCurrency(
        @PathVariable String playerId,
        @RequestBody WalletDTOs.BatchReq request
    );
}
