package com.SouthMillion.main_fb_service.grpc;

import com.SouthMillion.main_fb_service.service.MainFbService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.SouthMillion.dto.main_fb.*;
import org.SouthMillion.proto.mainfb.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for Main Dungeon/Copy service
 * Handles PvE instance/dungeon operations with high throughput
 */
@Service
@RequiredArgsConstructor
public class MainFbServiceGrpcImpl extends MainFbServiceGrpc.MainFbServiceImplBase {

    private final MainFbService mainFbService;

    @Override
    public void getCurrentTask(GetCurrentTaskRequest request, StreamObserver<GetCurrentTaskResponse> responseObserver) {
        try {
            TaskResp task = mainFbService.getCurrentTask(request.getPlayerId());
            
            GetCurrentTaskResponse.Builder builder = GetCurrentTaskResponse.newBuilder()
                    .setAllDone(task.getAllDone());
            
            if (!task.getAllDone()) {
                builder.setIndex(task.getIndex())
                        .setTitle(task.getTitle())
                        .setDesc(task.getDesc())
                        .setStage(task.getStage())
                        .setLevel(task.getLevel())
                        .setRequireScore(task.getRequireScore());
                
                if (task.getRewards() != null) {
                    task.getRewards().forEach(reward -> 
                        builder.addRewards(org.SouthMillion.proto.mainfb.RewardItem.newBuilder()
                                .setItemId(reward.getItemId())
                                .setCount(reward.getCount())
                                .build())
                    );
                }
            }
            
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get current task: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getProgress(GetProgressRequest request, StreamObserver<GetProgressResponse> responseObserver) {
        try {
            MainFbDTOs.ProgressResp progress = mainFbService.getProgress(request.getPlayerId());
            
            GetProgressResponse.Builder builder = GetProgressResponse.newBuilder();
            
            if (progress.getProgresses() != null) {
                progress.getProgresses().forEach(sp -> 
                    builder.addProgresses(StageProgress.newBuilder()
                            .setStage(sp.getStage())
                            .setLevel(sp.getLevel())
                            .setBestScore(sp.getBestScore() != null ? sp.getBestScore() : 0)
                            .setClearCount(sp.getClearCount() != null ? sp.getClearCount() : 0)
                            .build())
                );
            }
            
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get progress: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void enterStage(EnterStageRequest request, StreamObserver<EnterStageResponse> responseObserver) {
        try {
            MainFbDTOs.EnterStageReq enterReq = MainFbDTOs.EnterStageReq.builder()
                    .playerId(request.getPlayerId())
                    .stage(request.getStage())
                    .level(request.getLevel())
                    .build();
            
            MainFbDTOs.EnterStageResp enterResp = mainFbService.enter(enterReq);
            
            EnterStageResponse response = EnterStageResponse.newBuilder()
                    .setBattleId(enterResp.getBattleId())
                    .setSeed(enterResp.getSeed() != null ? enterResp.getSeed() : 0L)
                    .setExpireAt(enterResp.getExpireAt() != null ? enterResp.getExpireAt() : 0L)
                    .setCost(enterResp.getCost() != null ? enterResp.getCost() : 0)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to enter stage: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void finishStage(FinishStageRequest request, StreamObserver<FinishStageResponse> responseObserver) {
        try {
            MainFbDTOs.FinishStageReq finishReq = MainFbDTOs.FinishStageReq.builder()
                    .playerId(request.getPlayerId())
                    .battleId(request.getBattleId())
                    .stage(request.getStage())
                    .level(request.getLevel())
                    .score(request.getScore())
                    .build();
            
            List<ItemStackDTO> rewards = mainFbService.finish(finishReq);
            
            FinishStageResponse.Builder builder = FinishStageResponse.newBuilder();
            
            if (rewards != null) {
                rewards.forEach(reward -> 
                    builder.addRewards(org.SouthMillion.proto.mainfb.ItemStack.newBuilder()
                            .setItemId(reward.itemId())
                            .setCount(reward.count())
                            .build())
                );
            }
            
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to finish stage: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sweepStage(SweepStageRequest request, StreamObserver<SweepStageResponse> responseObserver) {
        try {
            MainFbDTOs.SweepReq sweepReq = MainFbDTOs.SweepReq.builder()
                    .playerId(request.getPlayerId())
                    .stage(request.getStage())
                    .level(request.getLevel())
                    .times(request.getTimes())
                    .build();
            
            MainFbDTOs.SweepResp sweepResp = mainFbService.sweep(sweepReq);
            
            SweepStageResponse.Builder builder = SweepStageResponse.newBuilder();
            
            if (sweepResp.getRewards() != null) {
                sweepResp.getRewards().forEach(reward -> 
                    builder.addRewards(org.SouthMillion.proto.mainfb.RewardItem.newBuilder()
                            .setItemId(reward.getItemId())
                            .setCount(reward.getCount())
                            .build())
                );
            }
            
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to sweep stage: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void claimChapterReward(ClaimChapterRewardRequest request, StreamObserver<ClaimChapterRewardResponse> responseObserver) {
        try {
            List<ItemStackDTO> rewards = mainFbService.claimChapterReward(
                    request.getPlayerId(),
                    request.getStage(),
                    request.getChapterLabel()
            );
            
            ClaimChapterRewardResponse.Builder builder = ClaimChapterRewardResponse.newBuilder();
            
            if (rewards != null) {
                rewards.forEach(reward -> 
                    builder.addRewards(org.SouthMillion.proto.mainfb.ItemStack.newBuilder()
                            .setItemId(reward.itemId())
                            .setCount(reward.count())
                            .build())
                );
            }
            
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to claim chapter reward: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}

