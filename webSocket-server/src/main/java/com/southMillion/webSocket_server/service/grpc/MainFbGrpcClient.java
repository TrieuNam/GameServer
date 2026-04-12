package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.mainfb.*;
import org.springframework.stereotype.Service;

/**
 * gRPC client for Main FB/Dungeon Service
 * Replaces MainFbFeign REST calls with gRPC for 50-60% performance improvement
 */
@Slf4j
@Service
public class MainFbGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MainFbGrpcClient.class);

    @GrpcClient("main-fb-service")
    private MainFbServiceGrpc.MainFbServiceBlockingStub mainFbServiceStub;

    private static boolean isUnavailable(StatusRuntimeException e) {
        return e.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    /**
     * Get player's current task
     */
    public GetCurrentTaskResponse getCurrentTask(String playerId) {
        try {
            GetCurrentTaskRequest request = GetCurrentTaskRequest.newBuilder()
                    .setPlayerId(playerId).build();
            GetCurrentTaskResponse response = mainFbServiceStub.getCurrentTask(request);
            log.debug("Got current task for player: {}, all done: {}", playerId, response.getAllDone());
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] getCurrentTask: main-fb-service unavailable");
            else log.error("Failed to get current task for player: {}", playerId, e);
            return GetCurrentTaskResponse.newBuilder().setAllDone(false).build();
        }
    }

    /**
     * Get player's stage progress
     */
    public GetProgressResponse getProgress(String playerId) {
        try {
            GetProgressRequest request = GetProgressRequest.newBuilder()
                    .setPlayerId(playerId).build();
            GetProgressResponse response = mainFbServiceStub.getProgress(request);
            log.debug("Got progress for player: {}, stages count: {}", playerId, response.getProgressesCount());
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] getProgress: main-fb-service unavailable");
            else log.error("Failed to get progress for player: {}", playerId, e);
            return GetProgressResponse.newBuilder().build();
        }
    }

    /**
     * Enter a stage/level
     */
    public EnterStageResponse enterStage(String playerId, int stage, int level) {
        try {
            EnterStageRequest request = EnterStageRequest.newBuilder()
                    .setPlayerId(playerId).setStage(stage).setLevel(level).build();
            EnterStageResponse response = mainFbServiceStub.enterStage(request);
            log.info("Player {} entered stage {}-{}, battle_id: {}", playerId, stage, level, response.getBattleId());
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] enterStage: main-fb-service unavailable");
            else log.error("Failed to enter stage {}-{} for player: {}", stage, level, playerId, e);
            return EnterStageResponse.newBuilder().build();
        }
    }

    /**
     * Finish a stage and receive rewards
     */
    public FinishStageResponse finishStage(String playerId, String battleId, int stage, int level, int score) {
        try {
            FinishStageRequest request = FinishStageRequest.newBuilder()
                    .setPlayerId(playerId).setBattleId(battleId)
                    .setStage(stage).setLevel(level).setScore(score).build();
            FinishStageResponse response = mainFbServiceStub.finishStage(request);
            log.info("Player {} finished stage {}-{} with score {}, rewards count: {}",
                    playerId, stage, level, score, response.getRewardsCount());
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] finishStage: main-fb-service unavailable");
            else log.error("Failed to finish stage {}-{} for player: {}", stage, level, playerId, e);
            return FinishStageResponse.newBuilder().build();
        }
    }

    /**
     * Sweep (quick clear) a stage
     */
    public SweepStageResponse sweepStage(String playerId, int stage, int level, int times) {
        try {
            SweepStageRequest request = SweepStageRequest.newBuilder()
                    .setPlayerId(playerId).setStage(stage).setLevel(level).setTimes(times).build();
            SweepStageResponse response = mainFbServiceStub.sweepStage(request);
            log.info("Player {} swept stage {}-{} x{} times, rewards count: {}",
                    playerId, stage, level, times, response.getRewardsCount());
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] sweepStage: main-fb-service unavailable");
            else log.error("Failed to sweep stage {}-{} for player: {}", stage, level, playerId, e);
            return SweepStageResponse.newBuilder().build();
        }
    }

    /**
     * Claim chapter completion reward
     */
    public ClaimChapterRewardResponse claimChapterReward(String playerId, int stage) {
        try {
            ClaimChapterRewardRequest request = ClaimChapterRewardRequest.newBuilder()
                    .setPlayerId(playerId).setStage(stage).build();
            ClaimChapterRewardResponse response = mainFbServiceStub.claimChapterReward(request);
            log.info("Player {} claimed chapter reward for stage {}", playerId, stage);
            return response;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-mainfb] claimChapterReward: main-fb-service unavailable");
            else log.error("Failed to claim chapter reward for stage {} for player: {}", stage, playerId, e);
            return ClaimChapterRewardResponse.newBuilder().build();
        }
    }
}
