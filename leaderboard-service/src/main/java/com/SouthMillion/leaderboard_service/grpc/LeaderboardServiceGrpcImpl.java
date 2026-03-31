package com.SouthMillion.leaderboard_service.grpc;

import com.SouthMillion.leaderboard_service.dto.LeaderboardDTO;
import com.SouthMillion.leaderboard_service.service.LeaderboardService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.leaderboard.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC server implementation for leaderboard-service.
 * Exposes GetLeaderboard, UpdateScore, and GetPlayerRank as gRPC endpoints.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceGrpcImpl extends LeaderboardGrpcServiceGrpc.LeaderboardGrpcServiceImplBase {

    private final LeaderboardService leaderboardService;

    // ── GetLeaderboard ──────────────────────────────────────────────────────

    @Override
    public void getLeaderboard(GetLeaderboardRequest request,
                               StreamObserver<GetLeaderboardResponse> responseObserver) {
        try {
            String roleId = request.getCurrentRoleId().isBlank() ? null : request.getCurrentRoleId();
            LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> svcResp =
                    leaderboardService.getLeaderboard(request.getRankingType(), roleId);

            GetLeaderboardResponse.Builder builder = GetLeaderboardResponse.newBuilder()
                    .setStatus(ok());

            if (svcResp.getData() != null) {
                LeaderboardDTO.LeaderboardResponse data = svcResp.getData();
                builder.setRankingType(data.getRankingType() != null ? data.getRankingType() : 0)
                       .setRankingName(data.getRankingName() != null ? data.getRankingName() : "")
                       .setTotalPlayers(data.getTotalPlayers() != null ? data.getTotalPlayers() : 0);

                if (data.getRankings() != null) {
                    List<RankingEntry> entries = data.getRankings().stream()
                            .map(this::toProto).collect(Collectors.toList());
                    builder.addAllRankings(entries);
                }
                if (data.getMyRank() != null) {
                    builder.setMyRank(toProto(data.getMyRank()));
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC getLeaderboard error: type={}", request.getRankingType(), e);
            responseObserver.onNext(GetLeaderboardResponse.newBuilder()
                    .setStatus(err(e.getMessage())).build());
            responseObserver.onCompleted();
        }
    }

    // ── UpdateScore ─────────────────────────────────────────────────────────

    @Override
    public void updateScore(UpdateScoreRequest request,
                            StreamObserver<UpdateScoreResponse> responseObserver) {
        try {
            LeaderboardDTO.UpdateScoreRequest dto = LeaderboardDTO.UpdateScoreRequest.builder()
                    .rankingType(request.getRankingType())
                    .roleId(request.getRoleId())
                    .roleName(request.getRoleName())
                    .roleLevel(request.getRoleLevel())
                    .score(request.getScore())
                    .guildName(request.getGuildName().isBlank() ? null : request.getGuildName())
                    .build();

            LeaderboardDTO.Response<LeaderboardDTO.RankingInfo> svcResp =
                    leaderboardService.updateScore(dto);

            UpdateScoreResponse.Builder builder = UpdateScoreResponse.newBuilder().setStatus(ok());
            if (svcResp.getData() != null) {
                builder.setEntry(toProto(svcResp.getData()));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC updateScore error: roleId={}", request.getRoleId(), e);
            responseObserver.onNext(UpdateScoreResponse.newBuilder()
                    .setStatus(err(e.getMessage())).build());
            responseObserver.onCompleted();
        }
    }

    // ── GetPlayerRank ────────────────────────────────────────────────────────

    @Override
    public void getPlayerRank(GetPlayerRankRequest request,
                              StreamObserver<GetPlayerRankResponse> responseObserver) {
        try {
            GetPlayerRankResponse.Builder builder = GetPlayerRankResponse.newBuilder().setStatus(ok());
            int rankingType = request.getRankingType();
            // If rankingType == 0 → query all 8 types; otherwise just the requested type
            int start = (rankingType == 0) ? 1 : rankingType;
            int end   = (rankingType == 0) ? 8 : rankingType;

            for (int type = start; type <= end; type++) {
                try {
                    LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> svcResp =
                            leaderboardService.getLeaderboard(type, request.getRoleId());
                    if (svcResp.getData() != null && svcResp.getData().getMyRank() != null) {
                        builder.addEntries(toProto(svcResp.getData().getMyRank()));
                    }
                } catch (Exception ignored) {
                    // skip types with no data
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC getPlayerRank error: roleId={}", request.getRoleId(), e);
            responseObserver.onNext(GetPlayerRankResponse.newBuilder()
                    .setStatus(err(e.getMessage())).build());
            responseObserver.onCompleted();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RankingEntry toProto(LeaderboardDTO.RankingInfo info) {
        return RankingEntry.newBuilder()
                .setRank(info.getRank()       != null ? info.getRank()       : 0)
                .setRoleId(info.getRoleId()   != null ? info.getRoleId()     : "")
                .setRoleName(info.getRoleName()!= null ? info.getRoleName()  : "")
                .setRoleLevel(info.getRoleLevel()!= null ? info.getRoleLevel(): 0)
                .setScore(info.getScore()     != null ? info.getScore()      : 0L)
                .setRankChange(info.getRankChange()!= null ? info.getRankChange(): 0)
                .setGuildName(info.getGuildName()!= null ? info.getGuildName(): "")
                .build();
    }

    private static ResponseStatus ok() {
        return ResponseStatus.newBuilder().setCode(200).setSuccess(true).setMessage("OK").build();
    }

    private static ResponseStatus err(String msg) {
        return ResponseStatus.newBuilder().setCode(500).setSuccess(false)
                .setMessage(msg != null ? msg : "Internal error").build();
    }
}
