package com.SouthMillion.crafting_service.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.wallet.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * gRPC Client để gọi wallet-service.
 *
 * Dùng gRPC (không dùng REST/Feign) để nhất quán với các inter-service calls khác.
 * - batchCost  → WalletService.BatchCost  (trừ coin khi chế tác)
 * - batchAdd   → WalletService.BatchAdd   (cộng coin khi nhận thưởng)
 * - getBalance → WalletService.GetBalances (kiểm tra số dư)
 */
@Slf4j
@Service
public class WalletGrpcClient {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletServiceStub;

    // ─────────────────────────────────────────────────────────────
    // BATCH COST  (trừ tiền — dùng trong startCraft)
    // ─────────────────────────────────────────────────────────────

    /**
     * Trừ tiền người chơi. Trả về false nếu không đủ tiền hoặc lỗi.
     *
     * @param roleId   người chơi
     * @param itemId   loại tiền (ví dụ: 1 = coin, 2 = diamond)
     * @param amount   số lượng cần trừ (> 0)
     * @param reason   mã lý do (ví dụ: 20 = CRAFT)
     * @return true nếu trừ thành công
     */
    public boolean deductCoin(long roleId, long itemId, long amount, int reason) {
        try {
            BatchCostRequest request = BatchCostRequest.newBuilder()
                    .setRoleId(roleId)
                    .addChanges(CurrencyChange.newBuilder()
                            .setItemId(itemId)
                            .setAmount(amount)
                            .build())
                    .setReason(reason)
                    .build();

            MutateResponse response = walletServiceStub.batchCost(request);
            if (response.getSuccess()) {
                log.info("[grpc-wallet] deductCoin OK roleId={} itemId={} amount={}", roleId, itemId, amount);
                return true;
            }
            log.warn("[grpc-wallet] deductCoin failed roleId={}: {}", roleId, response.getError());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-wallet] deductCoin error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // GET BALANCE  (kiểm tra số dư trước khi trừ)
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy số dư một loại tiền tệ của người chơi.
     *
     * @return số dư, hoặc -1 nếu lỗi
     */
    public long getBalance(long roleId, long itemId) {
        try {
            GetBalancesRequest request = GetBalancesRequest.newBuilder()
                    .setRoleId(roleId)
                    .addItemIds(itemId)
                    .build();

            BalancesResponse response = walletServiceStub.getBalances(request);
            return response.getBalancesList().stream()
                    .filter(e -> e.getItemId() == itemId)
                    .mapToLong(BalanceEntry::getBalance)
                    .findFirst()
                    .orElse(0L);
        } catch (StatusRuntimeException e) {
            log.error("[grpc-wallet] getBalance error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return -1L;
    }

    // ─────────────────────────────────────────────────────────────
    // BATCH ADD  (cộng tiền — dùng nếu cần hoàn tiền khi cancel)
    // ─────────────────────────────────────────────────────────────

    /**
     * Cộng tiền vào ví người chơi (ví dụ: hoàn coin khi cancel crafting).
     *
     * @return true nếu thành công
     */
    public boolean addCoin(long roleId, long itemId, long amount, int reason) {
        try {
            BatchAddRequest request = BatchAddRequest.newBuilder()
                    .setRoleId(roleId)
                    .addChanges(CurrencyChange.newBuilder()
                            .setItemId(itemId)
                            .setAmount(amount)
                            .build())
                    .setReason(reason)
                    .build();

            MutateResponse response = walletServiceStub.batchAdd(request);
            if (response.getSuccess()) {
                log.info("[grpc-wallet] addCoin OK roleId={} itemId={} amount={}", roleId, itemId, amount);
                return true;
            }
            log.warn("[grpc-wallet] addCoin failed roleId={}: {}", roleId, response.getError());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-wallet] addCoin error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return false;
    }
}

