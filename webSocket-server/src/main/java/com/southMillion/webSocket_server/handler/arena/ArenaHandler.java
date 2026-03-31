package com.SouthMillion.webSocket_server.handler.arena;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.grpc.ArenaGrpcClient;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgarena.Msgarena;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Arena Handler - Handles arena/PvP system operations via gRPC
 *
 * Protocol (theo proto gốc + MsgIdManger.ts):
 *   CS 9610 PB_CSArenaReq      → Arena thường
 *   CS 9613 PB_CSCrossArenaReq → Cross-server Arena
 *
 * SC responses:
 *   9611 PB_SCArenaInfo, 9612 PB_SCArenaReportList
 *   9614 PB_SCCrossArenaInfo,  9615 PB_SCCrossArenaReportList, 9616 PB_SCCrossArenaFightRet
 *
 * Arena ops (type field):
 *   1=GET_INFO  2=CHALLENGE(p1=opponentId)  3=GET_RANKING
 *   4=CLAIM_REWARDS  5=GET_OPPONENTS  6=BUY_CHALLENGE  7=GET_BATTLE_HISTORY
 *
 * Cross Arena ops (type field):
 *   0=GET_INFO  1=CHALLENGE(p1=targetIndex)  2=REFRESH  3=REVENGE(p1=targetIndex)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArenaHandler implements MessageHandler {

    private final ArenaGrpcClient arenaGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    @Override
    public int[] interests() {
        return new int[]{MessageIds.CS_ARENA_REQ, MessageIds.CS_CROSS_ARENA_REQ}; // 9610, 9613
    }

    /** Goi sau login: day thong tin dau truong (9611) ve client. */
    public Mono<Void> pushAll(PlayerSession session) {
        if (session.getRoleId() == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetArenaInfo(session));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        if (session.getRoleId() == null) {
            log.warn("[arena] User not logged in: sessionId={}", session.getSessionId());
            return;
        }

        log.info("[arena] Handle msgId={} for roleId={}", msgId, session.getRoleId());

        // Route Cross Arena (9613) separately
        if (msgId == MessageIds.CS_CROSS_ARENA_REQ) {
            handleCrossArenaMsg(session, payload);
            return;
        }

        try {
            Msgarena.PB_CSArenaReq req = Msgarena.PB_CSArenaReq.parseFrom(payload);
            int type = req.hasType() ? req.getType() : 1;
            int param = req.hasP1() ? req.getP1() : 0;

            switch (type) {
                case 1 -> handleGetArenaInfo(session);
                case 2 -> handleChallenge(session, param);
                case 3 -> handleGetRanking(session);
                case 4 -> handleClaimRewards(session);
                case 5 -> handleGetOpponents(session);
                case 6 -> handleBuyChallenge(session);
                case 7 -> handleGetBattleHistory(session);
                default -> log.warn("[arena] Unknown operation type: {}", type);
            }

        } catch (Exception e) {
            log.error("[arena] Error handling arena request", e);
        }
        });
    }

    // ─── Cross Arena (9613) ───────────────────────────────────────────────
    private void handleCrossArenaMsg(PlayerSession session, byte[] payload) {
        try {
            Msgarena.PB_CSCrossArenaReq req = Msgarena.PB_CSCrossArenaReq.parseFrom(payload);
            int type  = req.hasType() ? req.getType() : 0;
            int param = req.hasP1()   ? req.getP1()   : 0;
            log.info("[arena/cross] op={} roleId={}", type, session.getRoleId());
            switch (type) {
                case 0 -> handleCrossArenaGetInfo(session);
                case 1 -> handleCrossArenaChallenge(session, param);
                case 2 -> handleCrossArenaRefresh(session);
                case 3 -> handleCrossArenaRevenge(session, param);
                default -> log.warn("[arena/cross] Unknown op={}", type);
            }
        } catch (Exception e) {
            log.error("[arena/cross] parse error", e);
        }
    }

    private void handleCrossArenaGetInfo(PlayerSession session) {
        try {
            Map<String, Object> result = arenaGrpcClient.getArenaInfo(session.getRoleId());
            Msgarena.PB_SCCrossArenaInfo.Builder b = Msgarena.PB_SCCrossArenaInfo.newBuilder();
            if (result != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) result.get("player");
                if (player != null) {
                    Object rt = player.get("todayRefreshTimes");
                    if (rt instanceof Number) b.setTodayRefreshTimes(((Number) rt).intValue());
                }
            }
            Emitters.emit(session, MessageIds.SC_CROSS_ARENA_INFO, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[arena/cross] getInfo error", e);
            Emitters.emit(session, MessageIds.SC_CROSS_ARENA_INFO, Msgarena.PB_SCCrossArenaInfo.newBuilder().build().toByteArray());
        }
    }

    private void handleCrossArenaChallenge(PlayerSession session, int targetIndex) {
        try {
            Map<String, Object> result = arenaGrpcClient.challenge(session.getRoleId(), targetIndex);
            Msgarena.PB_SCCrossArenaFightRet.Builder b = Msgarena.PB_SCCrossArenaFightRet.newBuilder();
            if (result != null) {
                Object as = result.get("attackerScore");  if (as  instanceof Number) b.setAttackerScore(((Number)as).intValue());
                Object ac = result.get("attackerChange"); if (ac  instanceof Number) b.setAttackerChange(((Number)ac).intValue());
                Object ds = result.get("defenderScore");  if (ds  instanceof Number) b.setDefenderScore(((Number)ds).intValue());
                Object dc = result.get("defenderChange"); if (dc  instanceof Number) b.setDefenderChange(((Number)dc).intValue());
            }
            Emitters.emit(session, MessageIds.SC_CROSS_ARENA_FIGHT_RET, b.build().toByteArray());
            handleCrossArenaGetInfo(session); // refresh after fight
        } catch (Exception e) {
            log.error("[arena/cross] challenge error", e);
        }
    }

    private void handleCrossArenaRefresh(PlayerSession session) {
        try {
            arenaGrpcClient.getOpponents(session.getRoleId(), 5);
            handleCrossArenaGetInfo(session);
        } catch (Exception e) {
            log.error("[arena/cross] refresh error", e);
        }
    }

    private void handleCrossArenaRevenge(PlayerSession session, int targetIndex) {
        handleCrossArenaChallenge(session, targetIndex);
    }

    // ─── Arena SC — helper to reference correct 9611/9612 ────────────────
    /**
     * OP1: Get arena info
     */
    private void handleGetArenaInfo(PlayerSession session) {
        try {
            Map<String, Object> result = arenaGrpcClient.getArenaInfo(session.getRoleId());

            // Build response
            Msgarena.PB_SCArenaInfo.Builder builder = Msgarena.PB_SCArenaInfo.newBuilder();

            if (result != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) result.get("player");
                if (player != null) {
                    Object rank = player.get("rank");
                    if (rank instanceof Number) builder.setNowRank(((Number) rank).intValue());
                    Object rating = player.get("rating");
                    if (rating instanceof Number) builder.setNowScore(((Number) rating).intValue());
                }
                Object challenges = result.get("challengesRemaining");
                if (challenges instanceof Number) builder.setFightTimes(((Number) challenges).longValue());
            }

            // Send response
            Emitters.emit(session, MessageIds.SC_ARENA_INFO, builder.build().toByteArray());
            log.info("[arena] Sent arena info to roleId={}", session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to get arena info", e);
        }
    }

    /**
     * OP2: Challenge opponent
     */
    private void handleChallenge(PlayerSession session, int opponentId) {
        try {
            Map<String, Object> result = arenaGrpcClient.challenge(session.getRoleId(), opponentId);
            boolean victory = result != null && Boolean.TRUE.equals(result.get("victory"));
            if (victory) {
                publishTaskProgress(session.getRoleId(), taskActionConditionMapping.arenaWinTaskKey(), "websocket-arena-win");
            }

            // Send updated arena info
            handleGetArenaInfo(session);

            log.info("[arena] Challenge completed: roleId={}, opponent={}", 
                    session.getRoleId(), opponentId);

        } catch (Exception e) {
            log.error("[arena] Failed to challenge opponent={}", opponentId, e);
        }
    }

    /**
     * OP3: Get ranking — sends arena info with top-ranked opponents in roleinfo/target_rank/target_score
     */
    private void handleGetRanking(PlayerSession session) {
        try {
            List<Map<String, Object>> ranking = arenaGrpcClient.getRanking(1, 0, 50);

            Msgarena.PB_SCArenaInfo.Builder builder = Msgarena.PB_SCArenaInfo.newBuilder();

            if (ranking != null) {
                for (Map<String, Object> entry : ranking) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> player = entry.get("player") instanceof Map
                            ? (Map<String, Object>) entry.get("player") : entry;

                    // target_rank / target_score (repeated) for each opponent slot
                    Object rankVal = entry.get("rank");
                    if (rankVal instanceof Number) builder.addTargetRank(((Number) rankVal).intValue());
                    Object ratingVal = entry.get("rating");
                    if (ratingVal instanceof Number) builder.addTargetScore(((Number) ratingVal).intValue());

                    // Build role info for each ranking player
                    Msgrole.PB_RoleInfo.Builder roleInfo = Msgrole.PB_RoleInfo.newBuilder();
                    Object rid = player.get("roleId");
                    if (rid != null) try { roleInfo.setRoleId((int) Long.parseLong(rid.toString())); } catch (NumberFormatException ignored) {}
                    Object rname = player.get("roleName");
                    if (rname instanceof String && !((String) rname).isEmpty())
                        roleInfo.setName(com.google.protobuf.ByteString.copyFromUtf8((String) rname));
                    Object lv = player.get("level");
                    if (lv instanceof Number) roleInfo.setLevel(((Number) lv).intValue());
                    Object cap = player.get("power");
                    if (cap instanceof Number) roleInfo.setCap(((Number) cap).longValue());
                    builder.addRoleinfo(roleInfo.build());
                }
            }

            Emitters.emit(session, MessageIds.SC_ARENA_INFO, builder.build().toByteArray());
            log.info("[arena] Sent ranking ({} entries) to roleId={}", ranking != null ? ranking.size() : 0, session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to get ranking", e);
        }
    }

    /**
     * OP4: Claim rewards
     */
    private void handleClaimRewards(PlayerSession session) {
        try {
            Map<String, Object> result = arenaGrpcClient.claimRewards(session.getRoleId(), "DAILY");

            // Send updated arena info
            handleGetArenaInfo(session);

            log.info("[arena] Rewards claimed: roleId={}", session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to claim rewards", e);
        }
    }

    /**
     * OP5: Get opponents — sends arena info with matched opponents in roleinfo/target_rank/target_score
     */
    private void handleGetOpponents(PlayerSession session) {
        try {
            List<Map<String, Object>> opponents = arenaGrpcClient.getOpponents(session.getRoleId(), 5);

            // Re-fetch own info for context fields
            Map<String, Object> arenaResult = arenaGrpcClient.getArenaInfo(session.getRoleId());
            Msgarena.PB_SCArenaInfo.Builder builder = Msgarena.PB_SCArenaInfo.newBuilder();

            if (arenaResult != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) arenaResult.get("player");
                if (player != null) {
                    Object rank = player.get("rank");
                    if (rank instanceof Number) builder.setNowRank(((Number) rank).intValue());
                    Object rating = player.get("rating");
                    if (rating instanceof Number) builder.setNowScore(((Number) rating).intValue());
                }
                Object challenges = arenaResult.get("challengesRemaining");
                if (challenges instanceof Number) builder.setFightTimes(((Number) challenges).longValue());
            }

            if (opponents != null) {
                for (Map<String, Object> opp : opponents) {
                    Object rankVal = opp.get("rank");
                    if (rankVal instanceof Number) builder.addTargetRank(((Number) rankVal).intValue());
                    Object ratingVal = opp.get("rating");
                    if (ratingVal instanceof Number) builder.addTargetScore(((Number) ratingVal).intValue());

                    Msgrole.PB_RoleInfo.Builder roleInfo = Msgrole.PB_RoleInfo.newBuilder();
                    Object rid = opp.get("roleId");
                    if (rid != null) try { roleInfo.setRoleId((int) Long.parseLong(rid.toString())); } catch (NumberFormatException ignored) {}
                    Object rname = opp.get("roleName");
                    if (rname instanceof String && !((String) rname).isEmpty())
                        roleInfo.setName(com.google.protobuf.ByteString.copyFromUtf8((String) rname));
                    Object lv = opp.get("level");
                    if (lv instanceof Number) roleInfo.setLevel(((Number) lv).intValue());
                    Object cap = opp.get("power");
                    if (cap instanceof Number) roleInfo.setCap(((Number) cap).longValue());
                    builder.addRoleinfo(roleInfo.build());
                }
            }

            Emitters.emit(session, MessageIds.SC_ARENA_INFO, builder.build().toByteArray());
            log.info("[arena] Sent {} opponents to roleId={}", opponents != null ? opponents.size() : 0, session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to get opponents", e);
        }
    }

    /**
     * OP6: Buy challenge count
     */
    private void handleBuyChallenge(PlayerSession session) {
        try {
            Map<String, Object> result = arenaGrpcClient.buyChallengeCount(session.getRoleId(), 1);

            // Send updated arena info
            handleGetArenaInfo(session);

            log.info("[arena] Challenge count purchased: roleId={}", session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to buy challenge count", e);
        }
    }

    /**
     * OP7: Get battle history
     */
    private void handleGetBattleHistory(PlayerSession session) {
        try {
            List<Map<String, Object>> history = arenaGrpcClient.getBattleHistory(session.getRoleId(), 0, 10);

            Msgarena.PB_SCArenaReportList.Builder builder = Msgarena.PB_SCArenaReportList.newBuilder();

            if (history != null) {
                for (Map<String, Object> record : history) {
                    Msgarena.PB_ArenaReport.Builder report = Msgarena.PB_ArenaReport.newBuilder();

                    // Opponent info
                    Msgrole.PB_RoleInfo.Builder targetInfo = Msgrole.PB_RoleInfo.newBuilder();
                    boolean isAttack = Boolean.TRUE.equals(record.get("isAttack"))
                            || (record.get("attackerId") instanceof Number n && n.longValue() == session.getRoleId());
                    Object oppId = isAttack ? record.get("defenderId") : record.get("attackerId");
                    Object oppName = isAttack ? record.get("defenderName") : record.get("attackerName");
                    if (oppId instanceof Number) targetInfo.setRoleId(((Number) oppId).intValue());
                    if (oppName instanceof String && !((String) oppName).isEmpty())
                        targetInfo.setName(com.google.protobuf.ByteString.copyFromUtf8((String) oppName));
                    report.setTargetInfo(targetInfo.build());

                    report.setIsAttack(isAttack ? 1 : 0);

                    // Win/loss
                    Object isVictory = record.get("isVictory");
                    report.setIsWin(Boolean.TRUE.equals(isVictory) ? 1 : 0);

                    // Rating change
                    Object ratingChange = record.get("ratingChange");
                    if (ratingChange instanceof Number) report.setScore(((Number) ratingChange).intValue());

                    // Timestamp
                    Object battleTime = record.get("battleTime");
                    if (battleTime instanceof Number) report.setTime(((Number) battleTime).longValue());

                    builder.addReportList(report.build());
                }
            }

            Emitters.emit(session, MessageIds.SC_ARENA_REPORT_LIST, builder.build().toByteArray());
            publishTaskProgress(session.getRoleId(), "condition_37", "websocket-arena-history");
            log.info("[arena] Sent battle history ({} records) to roleId={}", history != null ? history.size() : 0, session.getRoleId());

        } catch (Exception e) {
            log.error("[arena] Failed to get battle history", e);
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}
