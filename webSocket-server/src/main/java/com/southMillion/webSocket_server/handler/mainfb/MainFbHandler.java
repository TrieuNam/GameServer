package com.SouthMillion.webSocket_server.handler.mainfb;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.MainFbGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgmainfb.Msgmainfb;
import org.SouthMillion.proto.mainfb.GetProgressResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Handles main story dungeon (主线副本) operations.
 *
 * Proto: PB_CSMainFbReq (2005) — type: 0=challenge/get info, 1=claim stage reward
 * Response: PB_SCMainFbInfo (2006) — level, stage, dia_fetch_num, last_fetch_time
 *
 * Uses MainFbGrpcClient for gRPC calls to main-fb-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainFbHandler implements MessageHandler {

    private final MainFbGrpcClient mainFbGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;

    private static final int OP_CHALLENGE    = 0;  // 挑战 / get info
    private static final int OP_CLAIM_REWARD = 1;  // 领取阶段奖励

    @Override
    public int[] interests() {
        return new int[]{2005}; // PB_CSMainFbReq
    }

    /** Gọi sau login: đẩy thông tin tiến độ chính tuyến (2006) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                GetProgressResponse prog = mainFbGrpcClient.getProgress(String.valueOf(roleId));
                int level = prog.getProgressesCount();
                Msgmainfb.PB_SCMainFbInfo info = Msgmainfb.PB_SCMainFbInfo.newBuilder()
                        .setLevel(level)
                        .setStage(level)
                        .build();
                sendInfo(session, info);
            } catch (Exception e) {
                log.warn("[MainFb] pushAll error for roleId={}: {}", roleId, e.getMessage());
                sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgmainfb.PB_CSMainFbReq req = Msgmainfb.PB_CSMainFbReq.parseFrom(payload);
                int type = req.hasType() ? req.getType() : OP_CHALLENGE;
                Long roleId = session.getRoleId();

                log.debug("[MainFb] op={}, roleId={}", type, roleId);

                switch (type) {
                    case OP_CHALLENGE    -> handleGetInfo(session, roleId);
                    case OP_CLAIM_REWARD -> handleClaimReward(session, roleId);
                    default -> {
                        log.warn("[MainFb] Unknown op={}", type);
                        sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
                    }
                }
            } catch (Exception e) {
                log.error("[MainFb] Error for roleId={}", session.getRoleId(), e);
                sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
            }
        });
    }

    // op=0: Get current main dungeon progress
    private void handleGetInfo(PlayerSession session, Long roleId) {
        try {
            GetProgressResponse prog = mainFbGrpcClient.getProgress(String.valueOf(roleId));
            Msgmainfb.PB_SCMainFbInfo info = Msgmainfb.PB_SCMainFbInfo.newBuilder()
                    .setLevel(prog.getProgressesCount())
                    .build();
            sendInfo(session, info);
        } catch (Exception e) {
            log.error("[MainFb] handleGetInfo error for roleId={}", roleId, e);
            sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
        }
    }

    // op=1: Claim current stage reward
    private void handleClaimReward(PlayerSession session, Long roleId) {
        try {
            GetProgressResponse prog = mainFbGrpcClient.getProgress(String.valueOf(roleId));
            int stage = prog.getProgressesCount() > 0 ? prog.getProgressesCount() : 1;
            mainFbGrpcClient.claimChapterReward(String.valueOf(roleId), stage);
            syncPostClaimState(session, roleId);
            reportTaskProgress(roleId, "complete_dungeon", 1);
            Msgmainfb.PB_SCMainFbInfo info = Msgmainfb.PB_SCMainFbInfo.newBuilder()
                    .setLevel(stage)
                    .setStage(stage)
                    .build();
            sendInfo(session, info);
        } catch (Exception e) {
            log.error("[MainFb] handleClaimReward error for roleId={}", roleId, e);
            sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
        }
    }

    private void syncPostClaimState(PlayerSession session, Long roleId) {
        pushBagState(session, roleId);
        pushWalletBalance(session, roleId);

        Mono.delay(Duration.ofMillis(250))
                .onErrorResume(ex -> Mono.empty())
                .subscribe(ignored -> {
                    pushBagState(session, roleId);
                    pushWalletBalance(session, roleId);
                });
    }

    private void pushBagState(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            List<BagDTOs.ItemView> list = bagFeign.list(String.valueOf(roleId));
            Emitters.sendKnapsackAllInfo(session, list);
        } catch (Exception e) {
            log.warn("[MainFb] Failed to push bag state for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private void pushWalletBalance(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(session, walletResp.balances());
            }
        } catch (Exception e) {
            log.warn("[MainFb] Failed to push wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private void sendInfo(PlayerSession session, Msgmainfb.PB_SCMainFbInfo info) {
        try {
            Emitters.emit(session, 2006, info.toByteArray());
        } catch (Exception e) {
            log.error("[MainFb] sendInfo failed", e);
        }
    }

    private void reportTaskProgress(Long roleId, String taskKey, int delta) {
        if (roleId == null || delta <= 0) {
            return;
        }
        try {
            taskProgressPublisher.publish(roleId, taskKey, delta, "websocket-mainfb");
        } catch (Exception e) {
            log.warn("[MainFb] Failed to report task progress roleId={}, taskKey={}: {}", roleId, taskKey, e.getMessage());
        }
    }
}
