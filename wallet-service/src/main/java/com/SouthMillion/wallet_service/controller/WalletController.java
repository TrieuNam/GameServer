package com.SouthMillion.wallet_service.controller;

import com.SouthMillion.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.ApiError;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Wallet API endpoints for players
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    
    private final WalletService walletService;

    /**
     * Get all balances for a player
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<?> getPlayerWallet(@PathVariable Long playerId) {
        try {
            WalletDTOs.BalancesResp balances = walletService.info(playerId);
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Get specific currency balance
     */
    @GetMapping("/{playerId}/{itemId}")
    public ResponseEntity<?> getCurrencyBalance(
            @PathVariable Long playerId,
            @PathVariable Long itemId) {
        try {
            WalletDTOs.BalancesResp balances = walletService.get(playerId, List.of(itemId));
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Add currency to player wallet
     */
    @PostMapping("/{playerId}/add")
    public ResponseEntity<?> addCurrency(
            @PathVariable Long playerId,
            @Valid @RequestBody WalletDTOs.BatchReq request) {
        try {
            // Ensure the request is for the correct player
            request.setRoleId(String.valueOf(playerId));
            WalletDTOs.MutateResp result = walletService.batchAdd(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Consume currency from player wallet
     */
    @PostMapping("/{playerId}/consume")
    public ResponseEntity<?> consumeCurrency(
            @PathVariable Long playerId,
            @Valid @RequestBody WalletDTOs.BatchReq request) {
        try {
            request.setRoleId(String.valueOf(playerId));
            WalletDTOs.MutateResp result = walletService.batchCost(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError("Insufficient funds: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Transfer currency between players
     */
    @PostMapping("/{playerId}/transfer")
    public ResponseEntity<?> transferCurrency(
            @PathVariable Long playerId,
            @RequestParam Long targetPlayerId,
            @RequestParam Long itemId,
            @RequestParam Long amount,
            @RequestParam(required = false) String reason) {
        try {
            // First consume from sender
            WalletDTOs.BatchReq consumeReq = new WalletDTOs.BatchReq();
            consumeReq.setRoleId(String.valueOf(playerId));
            consumeReq.setChanges(List.of(new WalletDTOs.Change(itemId, amount)));
            consumeReq.setReason(reason != null ? Integer.parseInt(reason) : 0);
            
            walletService.batchCost(consumeReq);
            
            // Then add to receiver
            WalletDTOs.BatchReq addReq = new WalletDTOs.BatchReq();
            addReq.setRoleId(String.valueOf(targetPlayerId));
            addReq.setChanges(List.of(new WalletDTOs.Change(itemId, amount)));
            addReq.setReason(reason != null ? Integer.parseInt(reason) : 0);
            
            WalletDTOs.MutateResp result = walletService.batchAdd(addReq);
            
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError("Transfer failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Bulk add currency operations (admin/system use)
     */
    @PostMapping("/bulk-add")
    public ResponseEntity<?> bulkAdd(@Valid @RequestBody WalletDTOs.BatchReq request) {
        try {
            WalletDTOs.MutateResp result = walletService.batchAdd(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }
}
