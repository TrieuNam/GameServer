package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "wallet-service", path = "/internal/wallet")
public interface WalletHttpClient {

    @PostMapping("/batch-add")
    ResultDTO<WalletDTOs.MutateResp> batchAdd(@RequestBody WalletDTOs.BatchReq req);

    @PostMapping("/batch-cost")
    ResultDTO<WalletDTOs.MutateResp> batchCost(@RequestBody WalletDTOs.BatchReq req);

    @GetMapping("/{roleId}")
    ResultDTO<WalletDTOs.BalancesResp> get(@PathVariable("roleId") String roleId,
                                           @RequestParam("itemIds") List<Long> itemIds);
}