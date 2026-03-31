package com.SouthMillion.arenaservice.grpc;

import com.SouthMillion.arenaservice.client.RoleFeignClient;
import com.SouthMillion.arenaservice.client.WalletFeignClient;
import com.SouthMillion.arenaservice.dto.ArenaPlayerDTO;
import com.SouthMillion.arenaservice.dto.BattleRequest;
import com.SouthMillion.arenaservice.dto.BattleResult;
import com.SouthMillion.arenaservice.entity.ArenaBattleHistory;
import com.SouthMillion.arenaservice.entity.ArenaPlayer;
import com.SouthMillion.arenaservice.service.ArenaEventPublisher;
import com.SouthMillion.arenaservice.service.ArenaService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.arena.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * gRPC Server Implementation for Arena Service
 * Publishes Kafka events for event-driven architecture
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ArenaServiceGrpcImpl extends ArenaServiceGrpc.ArenaServiceImplBase {

    private final ArenaService arenaService;
    private final ArenaEventPublisher eventPublisher;
    private final WalletFeignClient walletFeignClient;
    private final RoleFeignClient roleFeignClient;

    @Override
    public void getArenaInfo(GetArenaInfoRequest request, StreamObserver<ArenaInfoResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());
            ArenaPlayer player = arenaService.getOrCreatePlayer(playerId);

            ArenaInfoResponse response = ArenaInfoResponse.newBuilder()
                    .setPlayer(convertToProto(player))
                    .setChallengesRemaining(arenaService.getChallengesRemaining(playerId))
                    .setMaxChallenges(10)
                    .setNextResetTime(System.currentTimeMillis() / 1000 + 86400) // 24h from now
                    .setStatus(buildSuccessStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-arena] GetArenaInfo success for roleId={}", request.getRoleId());

        } catch (Exception e) {
            log.error("[grpc-arena] GetArenaInfo failed: {}", e.getMessage(), e);
            responseObserver.onNext(ArenaInfoResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getOpponents(GetOpponentsRequest request, StreamObserver<OpponentsResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());
            int count = request.getCount() > 0 ? request.getCount() : 5;

            // Use list-based query to return up to `count` distinct opponents in one DB call
            List<ArenaPlayer> opponents = arenaService.findOpponents(playerId, count);

            OpponentsResponse.Builder responseBuilder = OpponentsResponse.newBuilder();
            for (ArenaPlayer opponent : opponents) {
                responseBuilder.addOpponents(convertToProto(opponent));
            }

            responseBuilder.setStatus(buildSuccessStatus());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("[grpc-arena] GetOpponents success for roleId={}, returned={}/{}", request.getRoleId(), opponents.size(), count);

        } catch (Exception e) {
            log.error("[grpc-arena] GetOpponents failed: {}", e.getMessage(), e);
            responseObserver.onNext(OpponentsResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void startBattle(StartBattleRequest request, StreamObserver<BattleResultResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());
            String opponentId = request.getOpponentId() > 0 ? String.valueOf(request.getOpponentId()) : null;

            BattleRequest battleRequest = BattleRequest.builder()
                    .playerId(playerId)
                    .opponentId(opponentId)
                    .build();

            BattleResult result = arenaService.processBattle(battleRequest);

            boolean victory = result.getWinnerId().equals(playerId);
            int ratingChange = victory ? result.getRatingChange() : -result.getRatingChange();
            
            // Publish Kafka event for match end
            Long winnerId = Long.parseLong(result.getWinnerId());
            Long loserId = Long.parseLong(result.getLoserId());
            Map<String, Object> winnerInfo = fetchRoleInfo(result.getWinnerId());
            Map<String, Object> loserInfo  = fetchRoleInfo(result.getLoserId());
            eventPublisher.publishMatchEnd(
                    System.currentTimeMillis(), // matchId
                    winnerId,
                    roleName(winnerInfo, result.getWinnerId()),
                    result.getWinnerRatingBefore(),
                    result.getWinnerRatingAfter(),
                    loserId,
                    roleName(loserInfo, result.getLoserId()),
                    result.getLoserRatingBefore(),
                    result.getLoserRatingAfter(),
                    result.getBattleDuration() != null ? result.getBattleDuration() : 60,
                    "RANKED",
                    1, // seasonId
                    10000, // damage (calculated in ArenaService later)
                    8000,
                    false
            );

            BattleDetails battleDetails = BattleDetails.newBuilder()
                    .setAttackerId(Long.parseLong(playerId))
                    .setDefenderId(Long.parseLong(result.getLoserId().equals(playerId) ? result.getWinnerId() : result.getLoserId()))
                    .setAttackerRatingBefore(result.getWinnerRatingBefore())
                    .setAttackerRatingAfter(result.getWinnerRatingAfter())
                    .setDefenderRatingBefore(result.getLoserRatingBefore())
                    .setDefenderRatingAfter(result.getLoserRatingAfter())
                    .setBattleDuration(result.getBattleDuration() != null ? result.getBattleDuration() : 60)
                    .setIsConsecutiveWin(result.getIsConsecutiveWin() != null && result.getIsConsecutiveWin())
                    .setConsecutiveWinBonus(result.getConsecutiveWinBonus() != null ? result.getConsecutiveWinBonus() : 0)
                    .build();

            BattleResultResponse response = BattleResultResponse.newBuilder()
                    .setVictory(victory)
                    .setRatingChange(Math.abs(ratingChange))
                    .setNewRating(victory ? result.getWinnerRatingAfter() : result.getLoserRatingAfter())
                    .setNewRank(arenaService.calculatePlayerRank(playerId))
                    .setBattleDetails(battleDetails)
                    .setStatus(buildSuccessStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-arena] StartBattle success: roleId={} vs opponentId={}, victory={}",
                    request.getRoleId(), request.getOpponentId(), victory);

        } catch (Exception e) {
            log.error("[grpc-arena] StartBattle failed: {}", e.getMessage(), e);
            responseObserver.onNext(BattleResultResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getRankings(GetRankingsRequest request, StreamObserver<RankingsResponse> responseObserver) {
        try {
            int season = request.getSeason() > 0 ? request.getSeason() : 1;
            int page = request.getPage();
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Page<ArenaPlayerDTO> rankingsPage = arenaService.getRankings(season, page, size);

            RankingsResponse.Builder responseBuilder = RankingsResponse.newBuilder();
            int rank = page * size + 1;

            for (ArenaPlayerDTO playerDTO : rankingsPage.getContent()) {
                RankingEntry entry = RankingEntry.newBuilder()
                        .setRank(rank++)
                        .setPlayer(convertDTOToProto(playerDTO))
                        .setRating(playerDTO.getRating())
                        .setWins(playerDTO.getWins())
                        .setLosses(playerDTO.getLosses())
                        .build();
                responseBuilder.addRankings(entry);
            }

            responseBuilder
                    .setTotalCount((int) rankingsPage.getTotalElements())
                    .setPage(page)
                    .setSize(size)
                    .setStatus(buildSuccessStatus());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("[grpc-arena] GetRankings success: season={}, page={}, size={}", season, page, size);

        } catch (Exception e) {
            log.error("[grpc-arena] GetRankings failed: {}", e.getMessage(), e);
            responseObserver.onNext(RankingsResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void claimRewards(ClaimRewardsRequest request, StreamObserver<ClaimRewardsResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());

            // Calculate gold reward based on player's current rank tier
            long goldAmount = arenaService.calculateRankReward(playerId);

            // Grant gold via wallet service
            Map<String, Object> addReq = new HashMap<>();
            addReq.put("roleId", playerId);
            addReq.put("amount", goldAmount);
            addReq.put("currencyType", "gold");
            addReq.put("reason", "arena_rank_reward");
            walletFeignClient.addCurrency(addReq);

            // Build reward item (itemId=0 represents gold currency)
            Reward goldReward = Reward.newBuilder()
                    .setItemId(0)
                    .setAmount((int) goldAmount)
                    .setItemName("Gold")
                    .build();

            ClaimRewardsResponse response = ClaimRewardsResponse.newBuilder()
                    .addRewards(goldReward)
                    .setMessage("Claimed " + goldAmount + " gold")
                    .setStatus(buildSuccessStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-arena] ClaimRewards success: roleId={}, gold={}", request.getRoleId(), goldAmount);

        } catch (Exception e) {
            log.error("[grpc-arena] ClaimRewards failed: {}", e.getMessage(), e);
            responseObserver.onNext(ClaimRewardsResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getBattleHistory(GetBattleHistoryRequest request, StreamObserver<BattleHistoryResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());
            int page = request.getPage();
            int size = request.getSize() > 0 ? request.getSize() : 10;

            Page<ArenaBattleHistory> historyPage = arenaService.getPlayerBattleHistory(playerId, page, size);

            BattleHistoryResponse.Builder responseBuilder = BattleHistoryResponse.newBuilder();

            for (ArenaBattleHistory history : historyPage.getContent()) {
                boolean isVictory = history.getWinnerId().equals(playerId);

                // Determine who is attacker/defender based on battle order
                long attackerId = Long.parseLong(history.getPlayer1Id());
                long defenderId = Long.parseLong(history.getPlayer2Id());

                // Fetch real names; fall back to "Player-{id}" if role-service down
                Map<String, Object> attackerInfo = fetchRoleInfo(history.getPlayer1Id());
                Map<String, Object> defenderInfo = fetchRoleInfo(history.getPlayer2Id());

                BattleRecord record = BattleRecord.newBuilder()
                        .setBattleId(history.getBattleId())
                        .setAttackerId(attackerId)
                        .setDefenderId(defenderId)
                        .setAttackerName(roleName(attackerInfo, history.getPlayer1Id()))
                        .setDefenderName(roleName(defenderInfo, history.getPlayer2Id()))
                        .setIsVictory(isVictory)
                        .setRatingChange(history.getRatingChange() != null ? history.getRatingChange() : 0)
                        .setBattleTime(history.getTimestamp() != null ? history.getTimestamp().getEpochSecond() : 0)
                        .setSeason(1)
                        .build();

                responseBuilder.addBattles(record);
            }

            responseBuilder
                    .setTotalCount((int) historyPage.getTotalElements())
                    .setPage(page)
                    .setSize(size)
                    .setStatus(buildSuccessStatus());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("[grpc-arena] GetBattleHistory success for roleId={}", request.getRoleId());

        } catch (Exception e) {
            log.error("[grpc-arena] GetBattleHistory failed: {}", e.getMessage(), e);
            responseObserver.onNext(BattleHistoryResponse.newBuilder()
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void buyChallengeCount(BuyChallengeCountRequest request, StreamObserver<BuyChallengeCountResponse> responseObserver) {
        try {
            String playerId = String.valueOf(request.getRoleId());
            int count = request.getCount() > 0 ? request.getCount() : 1;
            long goldCost = (long) count * 50L; // 50 gold per extra challenge

            // Deduct gold from wallet (throws if insufficient)
            Map<String, Object> deductReq = new HashMap<>();
            deductReq.put("roleId", playerId);
            deductReq.put("amount", goldCost);
            deductReq.put("currencyType", "gold");
            deductReq.put("reason", "arena_buy_challenge");
            walletFeignClient.deductCurrency(deductReq);

            // Add extra challenges (decrements challengesUsedToday)
            int remaining = arenaService.addBoughtChallenges(playerId, count);

            BuyChallengeCountResponse response = BuyChallengeCountResponse.newBuilder()
                    .setSuccess(true)
                    .setNewChallengeCount(remaining)
                    .setCost((int) goldCost)
                    .setMessage("Bought " + count + " challenge(s) for " + goldCost + " gold")
                    .setStatus(buildSuccessStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-arena] BuyChallengeCount success: roleId={}, count={}, cost={}", request.getRoleId(), count, goldCost);

        } catch (Exception e) {
            log.error("[grpc-arena] BuyChallengeCount failed: {}", e.getMessage(), e);
            responseObserver.onNext(BuyChallengeCountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed: " + e.getMessage())
                    .setStatus(buildErrorStatus(e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    // Helper methods

    /**
     * Fetch lightweight role info from role-service.
     * Returns map with keys: name, level, fightPower.
     * Falls back gracefully if role-service is unavailable.
     */
    private Map<String, Object> fetchRoleInfo(String roleId) {
        try {
            Map<String, Object> info = roleFeignClient.getBasicInfo(roleId);
            return info != null ? info : Map.of();
        } catch (Exception e) {
            log.debug("[arena] Could not fetch role info for roleId={}: {}", roleId, e.getMessage());
            return Map.of();
        }
    }

    private String roleName(Map<String, Object> info, String roleId) {
        Object name = info.get("name");
        return (name != null && !name.toString().isBlank()) ? name.toString() : "Player-" + roleId;
    }

    private int roleLevel(Map<String, Object> info) {
        Object level = info.get("level");
        if (level instanceof Number n) return n.intValue();
        return 1;
    }

    private int rolePower(Map<String, Object> info) {
        Object power = info.get("fightPower");
        if (power instanceof Number n) return n.intValue();
        return 0;
    }

    private org.SouthMillion.grpc.arena.ArenaPlayer convertToProto(com.SouthMillion.arenaservice.entity.ArenaPlayer entity) {
        Map<String, Object> info = fetchRoleInfo(entity.getPlayerId());
        return org.SouthMillion.grpc.arena.ArenaPlayer.newBuilder()
                .setRoleId(Long.parseLong(entity.getPlayerId()))
                .setRoleName(roleName(info, entity.getPlayerId()))
                .setRating(entity.getRating() != null ? entity.getRating() : 1000)
                .setRank(entity.getCurrentRank() != null ? entity.getCurrentRank() : 0)
                .setLevel(roleLevel(info))
                .setPower(rolePower(info))
                .setWins(entity.getWins() != null ? entity.getWins() : 0)
                .setLosses(entity.getLosses() != null ? entity.getLosses() : 0)
                .setConsecutiveWins(entity.getConsecutiveWins() != null ? entity.getConsecutiveWins() : 0)
                .setSeason(1)
                .setWinRate(entity.getTotalBattles() > 0 ? (int)((double) entity.getWins() / entity.getTotalBattles() * 100) : 0)
                .build();
    }

    private org.SouthMillion.grpc.arena.ArenaPlayer convertDTOToProto(ArenaPlayerDTO dto) {
        Map<String, Object> info = fetchRoleInfo(dto.getPlayerId());
        return org.SouthMillion.grpc.arena.ArenaPlayer.newBuilder()
                .setRoleId(Long.parseLong(dto.getPlayerId()))
                .setRoleName(roleName(info, dto.getPlayerId()))
                .setRating(dto.getRating() != null ? dto.getRating() : 1000)
                .setRank(dto.getCurrentRank() != null ? dto.getCurrentRank() : 0)
                .setLevel(roleLevel(info))
                .setPower(rolePower(info))
                .setWins(dto.getWins() != null ? dto.getWins() : 0)
                .setLosses(dto.getLosses() != null ? dto.getLosses() : 0)
                .setConsecutiveWins(dto.getConsecutiveWins() != null ? dto.getConsecutiveWins() : 0)
                .setSeason(1)
                .setWinRate(dto.getTotalBattles() != null && dto.getTotalBattles() > 0 ?
                    (int)((double) dto.getWins() / dto.getTotalBattles() * 100) : 0)
                .build();
    }

    private ResponseStatus buildSuccessStatus() {
        return ResponseStatus.newBuilder()
                .setSuccess(true)
                .setCode(0)
                .setMessage("Success")
                .build();
    }

    private ResponseStatus buildErrorStatus(String message) {
        return ResponseStatus.newBuilder()
                .setSuccess(false)
                .setCode(500)
                .setMessage(message != null ? message : "Internal error")
                .build();
    }
}

