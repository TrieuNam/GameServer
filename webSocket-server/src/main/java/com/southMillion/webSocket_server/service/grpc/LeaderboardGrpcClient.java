package com.SouthMillion.webSocket_server.service.grpc;

import com.SouthMillion.webSocket_server.service.client.LeaderboardFeign;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.leaderboard.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for leaderboard-service.
 * Uses gRPC stub (leaderboard_service.proto) with REST fallback via LeaderboardFeign.
 */
@Slf4j
@Service
public class LeaderboardGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeaderboardGrpcClient.class);

    @GrpcClient("leaderboard-service")
    private LeaderboardGrpcServiceGrpc.LeaderboardGrpcServiceBlockingStub leaderboardStub;

    @Autowired(required = false)
    private LeaderboardFeign leaderboardFeign;

    /** Get power ranking (type=1) */
    public Object getPowerRanking(int page, int size) {
        return getLeaderboard(1, page, size, null);
    }

    /** Get level ranking (type=2) */
    public Object getLevelRanking(int page, int size) {
        return getLeaderboard(2, page, size, null);
    }

    /** Get player's rank across all ranking types */
    public Object getPlayerRanking(Long roleId) {
        try {
            log.debug("[grpc-rank] getPlayerRank: roleId={}", roleId);
            GetPlayerRankResponse resp = leaderboardStub.getPlayerRank(
                    GetPlayerRankRequest.newBuilder().setRoleId(String.valueOf(roleId)).setRankingType(0).build());
            return resp.getEntriesList();
        } catch (Exception e) {
            log.warn("[grpc-rank] gRPC getPlayerRank failed, falling back to REST: {}", e.getMessage());
            if (leaderboardFeign != null) {
                // fallback: lấy bảng xếp hạng type POWER (1) và kèm vị trí của player
                return leaderboardFeign.getLeaderboard(1, String.valueOf(roleId));
            }
            return null;
        }
    }

    /** Generic leaderboard query by ranking type */
    public Object getLeaderboard(int rankingType, int page, int size, String currentRoleId) {
        try {
            log.debug("[grpc-rank] getLeaderboard: type={}, page={}, size={}", rankingType, page, size);
            GetLeaderboardRequest.Builder req = GetLeaderboardRequest.newBuilder()
                    .setRankingType(rankingType)
                    .setPage(page)
                    .setSize(size > 0 ? size : 50);
            if (currentRoleId != null && !currentRoleId.isBlank()) {
                req.setCurrentRoleId(currentRoleId);
            }
            GetLeaderboardResponse resp = leaderboardStub.getLeaderboard(req.build());
            return resp.getRankingsList();
        } catch (Exception e) {
            log.warn("[grpc-rank] gRPC getLeaderboard(type={}) failed, falling back to REST: {}", rankingType, e.getMessage());
            if (leaderboardFeign != null) {
                return leaderboardFeign.getLeaderboard(rankingType, null);
            }
            return null;
        }
    }
}

