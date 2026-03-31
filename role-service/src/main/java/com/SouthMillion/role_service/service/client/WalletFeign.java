package com.SouthMillion.role_service.service.client;

import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service", path = "/internal/wallet", contextId = "RoleSkillWalletFeign")
public interface WalletFeign {

    @PostMapping("/batch-cost")
    ResultDTO<WalletDTOs.MutateResp> batchCost(@RequestBody WalletDTOs.BatchReq req);

    @PostMapping("/batch-add")
    ResultDTO<WalletDTOs.MutateResp> batchAdd(@RequestBody WalletDTOs.BatchReq req);
}
