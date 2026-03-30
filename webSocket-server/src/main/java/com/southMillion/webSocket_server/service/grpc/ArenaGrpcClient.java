package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.arena.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gRPC Client for arena-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaGrpcClient {

    @GrpcClient("arena-service")
    private ArenaServiceGrpc.ArenaServiceBlockingStub arenaServiceStub;

    private static boolean isUnavailable(StatusRuntimeException e) {
        return e.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    /**
     * Get arena info for player
     */
    public Map<String, Object> getArenaInfo(Long roleId) {
        try {
            GetArenaInfoRequest request = GetArenaInfoRequest.newBuilder()
                    .setRoleId(roleId)
                    .build();

            ArenaInfoResponse response = arenaServiceStub.getArenaInfo(request);

            if (response.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("player", convertPlayerToMap(response.getPlayer()));
                result.put("challengesRemaining", response.getChallengesRemaining());
                result.put("maxChallenges", response.getMaxChallenges());
                result.put("nextResetTime", response.getNextResetTime());
                
                log.info("[grpc-arena] Got arena info for roleId={}", roleId);
                return result;
            } else {
                log.warn("[grpc-arena] Failed to get arena info: {}", response.getStatus().getMessage());
                return null;
            }

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] getArenaInfo: arena-service unavailable");
            else log.error("[grpc-arena] Failed to get arena info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get opponents for matchmaking
     */
    public List<Map<String, Object>> getOpponents(Long roleId, int count) {
        try {
            GetOpponentsRequest request = GetOpponentsRequest.newBuilder()
                    .setRoleId(roleId)
                    .setCount(count)
                    .build();

            OpponentsResponse response = arenaServiceStub.getOpponents(request);

            if (response.getStatus().getSuccess()) {
                log.info("[grpc-arena] Got {} opponents for roleId={}", response.getOpponentsCount(), roleId);
                return response.getOpponentsList().stream()
                        .map(this::convertPlayerToMap)
                        .collect(Collectors.toList());
            } else {
                log.warn("[grpc-arena] Failed to get opponents: {}", response.getStatus().getMessage());
                return List.of();
            }

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] getOpponents: arena-service unavailable");
            else log.error("[grpc-arena] Failed to get opponents: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Challenge opponent in arena
     */
    public Map<String, Object> challenge(Long roleId, long opponentId) {
        try {
            StartBattleRequest request = StartBattleRequest.newBuilder()
                    .setRoleId(roleId)
                    .setOpponentId(opponentId)
                    .build();

            BattleResultResponse response = arenaServiceStub.startBattle(request);

            if (response.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("victory", response.getVictory());
                result.put("ratingChange", response.getRatingChange());
                result.put("newRating", response.getNewRating());
                result.put("newRank", response.getNewRank());
                result.put("battleDetails", convertBattleDetailsToMap(response.getBattleDetails()));
                
                log.info("[grpc-arena] Arena challenge: roleId={} vs opponentId={}, victory={}",
                        roleId, opponentId, response.getVictory());
                return result;
            } else {
                log.warn("[grpc-arena] Failed to challenge: {}", response.getStatus().getMessage());
                return null;
            }

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] challenge: arena-service unavailable");
            else log.error("[grpc-arena] Failed to challenge: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get arena ranking
     */
    public List<Map<String, Object>> getRanking(int season, int page, int size) {
        try {
            GetRankingsRequest request = GetRankingsRequest.newBuilder()
                    .setSeason(season)
                    .setPage(page)
                    .setSize(size)
                    .build();

            RankingsResponse response = arenaServiceStub.getRankings(request);

            if (response.getStatus().getSuccess()) {
                log.info("[grpc-arena] Got ranking: season={}, page={}, count={}", 
                        season, page, response.getRankingsCount());
                return response.getRankingsList().stream()
                        .map(this::convertRankingEntryToMap)
                        .collect(Collectors.toList());
            } else {
                log.warn("[grpc-arena] Failed to get ranking: {}", response.getStatus().getMessage());
                return List.of();
            }

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] getRanking: arena-service unavailable");
            else log.error("[grpc-arena] Failed to get ranking: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Claim arena rewards
     */
    public Map<String, Object> claimRewards(Long roleId, String rewardType) {
        try {
            ClaimRewardsRequest request = ClaimRewardsRequest.newBuilder()
                    .setRoleId(roleId)
                    .setRewardType(rewardType)
                    .build();

            ClaimRewardsResponse response = arenaServiceStub.claimRewards(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getStatus().getSuccess());
            result.put("message", response.getMessage());
            result.put("rewards", response.getRewardsList().stream()
                    .map(this::convertRewardToMap)
                    .collect(Collectors.toList()));
            
            log.info("[grpc-arena] Claimed rewards for roleId={}, type={}", roleId, rewardType);
            return result;

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] claimRewards: arena-service unavailable");
            else log.error("[grpc-arena] Failed to claim rewards: {}", e.getMessage());
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    /**
     * Get battle history
     */
    public List<Map<String, Object>> getBattleHistory(Long roleId, int page, int size) {
        try {
            GetBattleHistoryRequest request = GetBattleHistoryRequest.newBuilder()
                    .setRoleId(roleId)
                    .setPage(page)
                    .setSize(size)
                    .build();

            BattleHistoryResponse response = arenaServiceStub.getBattleHistory(request);

            if (response.getStatus().getSuccess()) {
                log.info("[grpc-arena] Got battle history for roleId={}, count={}", 
                        roleId, response.getBattlesCount());
                return response.getBattlesList().stream()
                        .map(this::convertBattleRecordToMap)
                        .collect(Collectors.toList());
            } else {
                log.warn("[grpc-arena] Failed to get battle history: {}", response.getStatus().getMessage());
                return List.of();
            }

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] getBattleHistory: arena-service unavailable");
            else log.error("[grpc-arena] Failed to get battle history: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Buy challenge count
     */
    public Map<String, Object> buyChallengeCount(Long roleId, int count) {
        try {
            BuyChallengeCountRequest request = BuyChallengeCountRequest.newBuilder()
                    .setRoleId(roleId)
                    .setCount(count)
                    .build();

            BuyChallengeCountResponse response = arenaServiceStub.buyChallengeCount(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("newChallengeCount", response.getNewChallengeCount());
            result.put("cost", response.getCost());
            result.put("message", response.getMessage());
            
            log.info("[grpc-arena] Bought challenge count for roleId={}, count={}", roleId, count);
            return result;

        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-arena] buyChallengeCount: arena-service unavailable");
            else log.error("[grpc-arena] Failed to buy challenge count: {}", e.getMessage());
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    // Helper methods to convert proto to Map

    private Map<String, Object> convertPlayerToMap(ArenaPlayer player) {
        Map<String, Object> map = new HashMap<>();
        map.put("roleId", String.valueOf(player.getRoleId()));
        map.put("roleName", player.getRoleName());
        map.put("rating", player.getRating());
        map.put("rank", player.getRank());
        map.put("level", player.getLevel());
        map.put("power", player.getPower());
        map.put("wins", player.getWins());
        map.put("losses", player.getLosses());
        map.put("consecutiveWins", player.getConsecutiveWins());
        map.put("season", player.getSeason());
        map.put("winRate", player.getWinRate());
        return map;
    }

    private Map<String, Object> convertRankingEntryToMap(RankingEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("rank", entry.getRank());
        map.put("player", convertPlayerToMap(entry.getPlayer()));
        map.put("rating", entry.getRating());
        map.put("wins", entry.getWins());
        map.put("losses", entry.getLosses());
        return map;
    }

    private Map<String, Object> convertBattleDetailsToMap(BattleDetails details) {
        Map<String, Object> map = new HashMap<>();
        map.put("attackerId", details.getAttackerId());
        map.put("defenderId", details.getDefenderId());
        map.put("attackerName", details.getAttackerName());
        map.put("defenderName", details.getDefenderName());
        map.put("attackerRatingBefore", details.getAttackerRatingBefore());
        map.put("attackerRatingAfter", details.getAttackerRatingAfter());
        map.put("defenderRatingBefore", details.getDefenderRatingBefore());
        map.put("defenderRatingAfter", details.getDefenderRatingAfter());
        map.put("battleDuration", details.getBattleDuration());
        map.put("isConsecutiveWin", details.getIsConsecutiveWin());
        map.put("consecutiveWinBonus", details.getConsecutiveWinBonus());
        return map;
    }

    private Map<String, Object> convertBattleRecordToMap(BattleRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("battleId", record.getBattleId());
        map.put("attackerId", record.getAttackerId());
        map.put("defenderId", record.getDefenderId());
        map.put("attackerName", record.getAttackerName());
        map.put("defenderName", record.getDefenderName());
        map.put("isVictory", record.getIsVictory());
        map.put("ratingChange", record.getRatingChange());
        map.put("battleTime", record.getBattleTime());
        map.put("season", record.getSeason());
        return map;
    }

    private Map<String, Object> convertRewardToMap(Reward reward) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", reward.getItemId());
        map.put("amount", reward.getAmount());
        map.put("itemName", reward.getItemName());
        return map;
    }
}
