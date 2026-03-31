package com.SouthMillion.wallet_service.grpc;

import com.SouthMillion.wallet_service.service.WalletService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.wallet.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gRPC Server cho wallet-service.
 *
 * Cung cấp các operations tiền tệ cho inter-service calls (gRPC thay thế REST):
 *  - BatchCost    → trừ tiền (crafting, shop, ...)
 *  - BatchAdd     → cộng tiền (reward, top-up, ...)
 *  - GetBalances  → truy vấn số dư
 *
 * REST controller vẫn giữ để dùng cho external/admin endpoints.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class WalletServiceGrpcImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;

    // ─────────────────────────────────────────────────────────────
    // BATCH COST  (trừ tiền)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void batchCost(BatchCostRequest request, StreamObserver<MutateResponse> responseObserver) {
        try {
            log.info("[grpc-wallet] BatchCost: roleId={} changes={}", request.getRoleId(), request.getChangesCount());

            WalletDTOs.BatchReq req = toDto(request.getRoleId(), request.getChangesList(),
                    request.getIdemKey(), request.getReason());

            WalletDTOs.MutateResp resp = walletService.batchCost(req);
            responseObserver.onNext(toMutateResponse(resp));
            responseObserver.onCompleted();

        } catch (IllegalStateException e) {
            // INSUFFICIENT_FUNDS
            log.warn("[grpc-wallet] BatchCost insufficient: roleId={} err={}", request.getRoleId(), e.getMessage());
            responseObserver.onNext(MutateResponse.newBuilder()
                    .setSuccess(false)
                    .setError(e.getMessage())
                    .setStatus(ResponseStatus.newBuilder().setCode(400).setSuccess(false).setMessage(e.getMessage()).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[grpc-wallet] BatchCost error roleId={}", request.getRoleId(), e);
            responseObserver.onError(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BATCH ADD  (cộng tiền)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void batchAdd(BatchAddRequest request, StreamObserver<MutateResponse> responseObserver) {
        try {
            log.info("[grpc-wallet] BatchAdd: roleId={} changes={}", request.getRoleId(), request.getChangesCount());

            WalletDTOs.BatchReq req = toDto(request.getRoleId(), request.getChangesList(),
                    request.getIdemKey(), request.getReason());

            WalletDTOs.MutateResp resp = walletService.batchAdd(req);
            responseObserver.onNext(toMutateResponse(resp));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[grpc-wallet] BatchAdd error roleId={}", request.getRoleId(), e);
            responseObserver.onError(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET BALANCES
    // ─────────────────────────────────────────────────────────────

    @Override
    public void getBalances(GetBalancesRequest request, StreamObserver<BalancesResponse> responseObserver) {
        try {
            log.debug("[grpc-wallet] GetBalances: roleId={}", request.getRoleId());

            WalletDTOs.BalancesResp resp = walletService.get(request.getRoleId(), request.getItemIdsList());

            BalancesResponse.Builder builder = BalancesResponse.newBuilder()
                    .setRoleId(request.getRoleId())
                    .setAtEpochSec(resp.getAtEpochSec() != null ? resp.getAtEpochSec() : 0L)
                    .setStatus(ResponseStatus.newBuilder().setCode(200).setSuccess(true).build());

            if (resp.getBalances() != null) {
                resp.getBalances().forEach((itemId, balance) ->
                        builder.addBalances(BalanceEntry.newBuilder()
                                .setItemId(itemId)
                                .setBalance(balance)
                                .build()));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[grpc-wallet] GetBalances error roleId={}", request.getRoleId(), e);
            responseObserver.onError(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private WalletDTOs.BatchReq toDto(long roleId, List<CurrencyChange> changes, String idemKey, int reason) {
        List<WalletDTOs.Change> dtoChanges = changes.stream()
                .map(c -> WalletDTOs.Change.builder()
                        .itemId(c.getItemId())
                        .amount(c.getAmount())
                        .build())
                .collect(Collectors.toList());
        return WalletDTOs.BatchReq.builder()
                .roleId(String.valueOf(roleId))
                .changes(dtoChanges)
                .idemKey(idemKey.isBlank() ? null : idemKey)
                .reason(reason > 0 ? reason : null)
                .build();
    }

    private MutateResponse toMutateResponse(WalletDTOs.MutateResp resp) {
        MutateResponse.Builder builder = MutateResponse.newBuilder()
                .setSuccess(resp.isOk())
                .setError(resp.getError() != null ? resp.getError() : "")
                .setAtEpochSec(resp.getAtEpochSec() != null ? resp.getAtEpochSec() : 0L)
                .setStatus(ResponseStatus.newBuilder()
                        .setCode(resp.isOk() ? 200 : 400)
                        .setSuccess(resp.isOk())
                        .setMessage(resp.getError() != null ? resp.getError() : "")
                        .build());

        if (resp.getNewBalances() != null) {
            resp.getNewBalances().forEach((itemId, balance) ->
                    builder.addNewBalances(BalanceEntry.newBuilder()
                            .setItemId(itemId)
                            .setBalance(balance)
                            .build()));
        }
        return builder.build();
    }
}

