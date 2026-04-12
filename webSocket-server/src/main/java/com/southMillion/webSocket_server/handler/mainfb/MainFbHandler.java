package com.SouthMillion.webSocket_server.handler.mainfb;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.MainFbGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgbattle.Msgbattle;
import org.SouthMillion.proto.Msgmainfb.Msgmainfb;
import org.SouthMillion.proto.mainfb.EnterStageResponse;
import org.SouthMillion.proto.mainfb.FinishStageResponse;
import org.SouthMillion.proto.mainfb.GetCurrentTaskResponse;
import org.SouthMillion.proto.mainfb.GetProgressResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MainFbHandler.class);

    private final MainFbGrpcClient mainFbGrpcClient;
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;

    private static final int OP_CHALLENGE         = 0;      // 挑战当前主线关卡
    private static final int OP_CLAIM_REWARD      = 1;      // 领取阶段奖励
    private static final int OP_FINISH            = 5;      // 上报战斗结果 (WIN)
    private static final int MSG_SC_MAIN_FB_INFO  = 2006;
    private static final int MSG_SC_BATTLE_REPORT = 11003;
    private static final int MAIN_FB_BATTLE_MODE  = 0;

    /** Pending battle info stored when challenge starts; consumed when finish arrives. */
    private final Map<Long, PendingBattle> pendingBattles = new ConcurrentHashMap<>();
    private record PendingBattle(String battleId, int stage, int level) {}

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
                sendInfo(session, buildProgressInfo(roleId));
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
                    case OP_CHALLENGE    -> handleChallenge(session, roleId);
                    case OP_CLAIM_REWARD -> handleClaimReward(session, roleId);
                    case OP_FINISH       -> handleFinish(session, roleId);
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

    // op=0: Start the current main-fb challenge and return battle data for the client.
    private void handleChallenge(PlayerSession session, Long roleId) {
        if (roleId == null) {
            sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
            return;
        }

        String playerId = String.valueOf(roleId);
        try {
            GetCurrentTaskResponse currentTask = mainFbGrpcClient.getCurrentTask(playerId);
            if (currentTask == null || currentTask.getAllDone()) {
                log.info("[MainFb] No pending main-fb stage for roleId={}", roleId);
                sendInfo(session, buildProgressInfo(roleId));
                return;
            }

            int stage = currentTask.getStage();
            int level = currentTask.getLevel();
            if (stage <= 0 || level <= 0) {
                log.warn("[MainFb] Invalid task stage for roleId={}, stage={}, level={}", roleId, stage, level);
                sendInfo(session, buildProgressInfo(roleId));
                return;
            }

            EnterStageResponse enterResp = mainFbGrpcClient.enterStage(playerId, stage, level);
            if (enterResp == null || enterResp.getBattleId() == null || enterResp.getBattleId().isBlank()) {
                log.warn("[MainFb] enterStage returned empty battleId for roleId={}, stage={}, level={}", roleId, stage, level);
                sendInfo(session, buildProgressInfo(roleId));
                return;
            }

            Msgbattle.PB_SCBattleReport report = Msgbattle.PB_SCBattleReport.newBuilder()
                    .setBattleModeType(MAIN_FB_BATTLE_MODE)
                    .setBattleFileName(enterResp.getBattleId())
                    .build();
            Emitters.emit(session, MSG_SC_BATTLE_REPORT, report.toByteArray());
            pendingBattles.put(roleId, new PendingBattle(enterResp.getBattleId(), stage, level));
            log.info("[MainFb] Started stage {}-{} for roleId={}, battleId={}", stage, level, roleId, enterResp.getBattleId());
        } catch (Exception e) {
            log.error("[MainFb] handleChallenge error for roleId={}", roleId, e);
            sendInfo(session, buildProgressInfo(roleId));
        }
    }

    // op=5: Client reports WIN result — update progress, grant rewards, advance task
    private void handleFinish(PlayerSession session, Long roleId) {
        PendingBattle pending = pendingBattles.remove(roleId);
        if (pending == null) {
            log.warn("[MainFb] handleFinish: no pending battle for roleId={}", roleId);
            sendInfo(session, buildProgressInfo(roleId));
            return;
        }
        try {
            FinishStageResponse resp = mainFbGrpcClient.finishStage(
                    String.valueOf(roleId), pending.battleId(), pending.stage(), pending.level(), 0);
            
            // Build and send battle report with rewards
            sendBattleReportWithRewards(session, resp);
            
            syncPostClaimState(session, roleId);
            sendInfo(session, buildProgressInfo(roleId));
            log.info("[MainFb] Finished stage {}-{} for roleId={}, battleId={}, rewards={}",
                    pending.stage(), pending.level(), roleId, pending.battleId(), resp.getRewardsCount());
        } catch (Exception e) {
            log.error("[MainFb] handleFinish error for roleId={}", roleId, e);
            sendInfo(session, buildProgressInfo(roleId));
        }
    }

    // Helper method to construct and send battle report with rewards
    private void sendBattleReportWithRewards(PlayerSession session, FinishStageResponse resp) {
        try {
            Msgbattle.PB_SCBattleReport.Builder reportBuilder = Msgbattle.PB_SCBattleReport.newBuilder()
                    .setBattleResultType(1)  // 1 = WIN (only called on victory)
                    .setBattleModeType(MAIN_FB_BATTLE_MODE)
                    .setTotalDamage(0);  // MainFb doesn't track battle damage
            
            // Map rewards from FinishStageResponse to PB_SCBattleReport
            if (resp != null && resp.getRewardsCount() > 0) {
                resp.getRewardsList().forEach(itemStack -> {
                    int rewardItemId = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, itemStack.getItemId()));
                    int rewardNum = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, itemStack.getCount()));
                    reportBuilder.addRewardList(
                        Msgbattle.PB_SCBattleReward.newBuilder()
                            .setItemId(rewardItemId)
                            .setItemNum(rewardNum)
                            .build()
                    );
                });
            }
            
            Msgbattle.PB_SCBattleReport report = reportBuilder.build();
            Emitters.emit(session, MSG_SC_BATTLE_REPORT, report.toByteArray());
            log.debug("[MainFb] Sent battle report with {} rewards to roleId={}", 
                    resp.getRewardsCount(), session.getRoleId());
        } catch (Exception e) {
            log.warn("[MainFb] Failed to send battle report: {}", e.getMessage());
        }
    }

    // Retained for explicit info refresh flows if needed.
    private void handleGetInfo(PlayerSession session, Long roleId) {
        try {
            sendInfo(session, buildProgressInfo(roleId));
        } catch (Exception e) {
            log.error("[MainFb] handleGetInfo error for roleId={}", roleId, e);
            sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
        }
    }

    // op=1: Claim current stage reward
    private void handleClaimReward(PlayerSession session, Long roleId) {
        try {
            String playerId = String.valueOf(roleId);
            int stage = resolveClaimStage(playerId);
            mainFbGrpcClient.claimChapterReward(playerId, stage);
            syncPostClaimState(session, roleId);
            sendInfo(session, buildProgressInfo(roleId));
        } catch (Exception e) {
            log.error("[MainFb] handleClaimReward error for roleId={}", roleId, e);
            sendInfo(session, Msgmainfb.PB_SCMainFbInfo.newBuilder().build());
        }
    }

    private Msgmainfb.PB_SCMainFbInfo buildProgressInfo(Long roleId) {
        if (roleId == null) {
            return Msgmainfb.PB_SCMainFbInfo.newBuilder().build();
        }

        String playerId = String.valueOf(roleId);
        GetProgressResponse prog = mainFbGrpcClient.getProgress(playerId);
        int clearedLevelCount = prog.getProgressesCount();
        int clearedStageCount = 0;

        try {
            GetCurrentTaskResponse currentTask = mainFbGrpcClient.getCurrentTask(playerId);
            if (currentTask != null) {
                if (currentTask.getAllDone()) {
                    clearedStageCount = Math.max(clearedStageCount, 1);
                } else if (currentTask.getStage() > 0) {
                    clearedStageCount = Math.max(0, currentTask.getStage() - 1);
                }
            }
        } catch (Exception e) {
            log.debug("[MainFb] buildProgressInfo fallback for roleId={}: {}", roleId, e.getMessage());
        }

        return Msgmainfb.PB_SCMainFbInfo.newBuilder()
                .setLevel(clearedLevelCount)
                .setStage(clearedStageCount)
                .build();
    }

    private int resolveClaimStage(String playerId) {
        try {
            GetCurrentTaskResponse currentTask = mainFbGrpcClient.getCurrentTask(playerId);
            if (currentTask != null) {
                if (currentTask.getAllDone()) {
                    return Math.max(1, currentTask.getStage());
                }
                if (currentTask.getStage() > 1) {
                    return currentTask.getStage() - 1;
                }
            }
        } catch (Exception e) {
            log.debug("[MainFb] resolveClaimStage fallback for playerId={}: {}", playerId, e.getMessage());
        }

        GetProgressResponse prog = mainFbGrpcClient.getProgress(playerId);
        return prog.getProgressesCount() > 0 ? prog.getProgressesCount() : 1;
    }

    private void syncPostClaimState(PlayerSession session, Long roleId) {
        pushBagState(session, roleId);
        pushWalletBalance(session, roleId);
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
            Emitters.emit(session, MSG_SC_MAIN_FB_INFO, info.toByteArray());
        } catch (Exception e) {
            log.error("[MainFb] sendInfo failed", e);
        }
    }

}
