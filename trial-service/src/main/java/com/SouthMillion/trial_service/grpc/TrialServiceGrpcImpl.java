package com.SouthMillion.trial_service.grpc;

import com.SouthMillion.trial_service.model.entity.TrialRecord;
import com.SouthMillion.trial_service.service.TrialEventPublisher;
import com.SouthMillion.trial_service.service.TrialService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.trial.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Server Implementation for Trial Service
 * Handles trial/challenge tower progression
 * Publishes Kafka events for event-driven architecture
 * 
 * Target: <15ms per RPC call
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TrialServiceGrpcImpl extends TrialServiceGrpc.TrialServiceImplBase {

    private final TrialService trialService;
    private final TrialEventPublisher eventPublisher;

    /** Configurable reward EXP per trial stage clear (default 1000) */
    @Value("${trial.reward.exp:1000}")
    private int defaultRewardExp;

    /** Maximum daily attempts per trial (default 10) */
    @Value("${trial.max-daily-attempts:10}")
    private int configuredMaxDailyAttempts;

    @Override
    public void getTrialRecord(GetTrialRecordRequest request, 
                              StreamObserver<TrialRecordResponse> responseObserver) {
        try {
            log.debug("[grpc-trial] GetTrialRecord: userId={}, trialId={}", 
                    request.getUserId(), request.getTrialId());
            
            TrialRecord record = trialService.getRecord(request.getUserId(), request.getTrialId());
            
            TrialRecordResponse response = TrialRecordResponse.newBuilder()
                    .setRecord(convertToProto(record))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] GetTrialRecord failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get trial record: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getAllTrialRecords(GetAllTrialRecordsRequest request, 
                                  StreamObserver<AllTrialRecordsResponse> responseObserver) {
        try {
            log.debug("[grpc-trial] GetAllTrialRecords: userId={}", request.getUserId());
            
            List<TrialRecord> records = trialService.getAllRecords(request.getUserId());
            
            AllTrialRecordsResponse response = AllTrialRecordsResponse.newBuilder()
                    .addAllRecords(records.stream()
                            .map(this::convertToProto)
                            .collect(Collectors.toList()))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] GetAllTrialRecords failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get trial records: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void startTrial(StartTrialRequest request, 
                         StreamObserver<TrialRecordResponse> responseObserver) {
        try {
            log.info("[grpc-trial] StartTrial: userId={}, trialId={}", 
                    request.getUserId(), request.getTrialId());
            
            TrialRecord record = trialService.startTrial(request.getUserId(), request.getTrialId());
            
            TrialRecordResponse response = TrialRecordResponse.newBuilder()
                    .setRecord(convertToProto(record))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] StartTrial failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to start trial: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void completeTrial(CompleteTrialRequest request, 
                            StreamObserver<TrialRecordResponse> responseObserver) {
        try {
            log.info("[grpc-trial] CompleteTrial: userId={}, trialId={}, score={}, stars={}", 
                    request.getUserId(), request.getTrialId(), request.getScore(), request.getStars());
            
            TrialRecord record = trialService.completeTrial(
                    request.getUserId(), 
                    request.getTrialId(),
                    request.getScore(),
                    request.getStars(),
                    request.getCompletionTime()
            );
            
            // Publish Kafka event
            eventPublisher.publishTrialCompleted(
                    request.getUserId(),
                    request.getUserId(), // roleId same as userId for now
                    request.getTrialId(),
                    record.getCurrentStage(),
                    request.getScore(),
                    request.getStars(),
                    request.getCompletionTime(),
                    record.getBestScore() == request.getScore(), // isNewRecord
                    record.getTotalAttempts()
            );
            
            TrialRecordResponse response = TrialRecordResponse.newBuilder()
                    .setRecord(convertToProto(record))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] CompleteTrial failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to complete trial: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void failTrial(FailTrialRequest request, 
                        StreamObserver<TrialRecordResponse> responseObserver) {
        try {
            log.info("[grpc-trial] FailTrial: userId={}, trialId={}", 
                    request.getUserId(), request.getTrialId());
            
            TrialRecord record = trialService.failTrial(request.getUserId(), request.getTrialId());
            
            // Publish Kafka event
            eventPublisher.publishTrialFailed(
                    request.getUserId(),
                    request.getUserId(), // roleId
                    request.getTrialId(),
                    record.getCurrentStage(),
                    "DEFEATED", // fail reason
                    record.getTotalAttempts()
            );
            
            TrialRecordResponse response = TrialRecordResponse.newBuilder()
                    .setRecord(convertToProto(record))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] FailTrial failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to fail trial: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void advanceStage(AdvanceStageRequest request, 
                           StreamObserver<TrialRecordResponse> responseObserver) {
        try {
            log.info("[grpc-trial] AdvanceStage: userId={}, trialId={}", 
                    request.getUserId(), request.getTrialId());
            
            TrialRecord record = trialService.advanceStage(request.getUserId(), request.getTrialId());
            
            TrialRecordResponse response = TrialRecordResponse.newBuilder()
                    .setRecord(convertToProto(record))
                    .setStatus(buildSuccessStatus())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] AdvanceStage failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to advance stage: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void claimReward(ClaimRewardRequest request, 
                          StreamObserver<ClaimRewardResponse> responseObserver) {
        try {
            log.info("[grpc-trial] ClaimReward: userId={}, trialId={}, stageId={}", 
                    request.getUserId(), request.getTrialId(), request.getStageId());
            
            boolean success = trialService.claimReward(
                    request.getUserId(), 
                    request.getTrialId(), 
                    request.getStageId()
            );
            
            ClaimRewardResponse.Builder responseBuilder = ClaimRewardResponse.newBuilder()
                    .setSuccess(success)
                    .setStatus(buildSuccessStatus());
            
            // Reward exp loaded from application config: trial.reward.exp (default 1000)
            if (success) {
                responseBuilder.setRewards(org.SouthMillion.grpc.common.RewardBundle.newBuilder()
                        .setExp(defaultRewardExp)
                        .build());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] ClaimReward failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to claim reward: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void resetDailyAttempts(ResetDailyAttemptsRequest request, 
                                  StreamObserver<ResponseStatus> responseObserver) {
        try {
            log.info("[grpc-trial] ResetDailyAttempts: userId={}, trialId={}", 
                    request.getUserId(), request.getTrialId());
            
            if (request.getTrialId() == 0) {
                // Reset all trials
                trialService.resetAllDailyAttempts(request.getUserId());
            } else {
                // Reset specific trial
                trialService.resetDailyAttempts(request.getUserId(), request.getTrialId());
            }
            
            responseObserver.onNext(buildSuccessStatus());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[grpc-trial] ResetDailyAttempts failed", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to reset daily attempts: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ========== CONVERSION METHODS ==========

    /**
     * Convert JPA entity to protobuf message
     */
    private org.SouthMillion.grpc.trial.TrialRecord convertToProto(TrialRecord entity) {
        if (entity == null) {
            return org.SouthMillion.grpc.trial.TrialRecord.getDefaultInstance();
        }
        
        org.SouthMillion.grpc.trial.TrialRecord.Builder builder = 
                org.SouthMillion.grpc.trial.TrialRecord.newBuilder()
                        .setUserId(entity.getUserId())
                        .setTrialId(entity.getTrialId())
                        .setCurrentStage(entity.getCurrentStage() != null ? entity.getCurrentStage() : 1)
                        .setBestStage(entity.getBestStage() != null ? entity.getBestStage() : 1)
                        .setBestScore(entity.getBestScore() != null ? entity.getBestScore() : 0)
                        .setTotalAttempts(entity.getTotalAttempts() != null ? entity.getTotalAttempts() : 0)
                        .setDailyAttempts(entity.getRemainingAttempts() != null ? entity.getRemainingAttempts() : 0)
                        .setMaxDailyAttempts(configuredMaxDailyAttempts); // configured via trial.max-daily-attempts
        
        // Status
        if (entity.getIsInProgress() != null && entity.getIsInProgress()) {
            builder.setStatus("IN_PROGRESS");
        } else if (entity.getCurrentStage() != null && entity.getCurrentStage() > 1) {
            builder.setStatus("COMPLETED");
        } else {
            builder.setStatus("NOT_STARTED");
        }
        
        // Timestamps
        if (entity.getUpdatedAt() != null) {
            builder.setLastAttemptTime(
                    entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            );
        }
        if (entity.getCreatedAt() != null) {
            builder.setCreatedAt(
                    entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            );
        }
        if (entity.getUpdatedAt() != null) {
            builder.setUpdatedAt(
                    entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            );
        }
        
        // Claimed rewards (bit flags to list)
        if (entity.getRewardsClaimed() != null && entity.getRewardsClaimed() > 0) {
            long flags = entity.getRewardsClaimed();
            for (int i = 0; i < 64; i++) {
                if ((flags & (1L << i)) != 0) {
                    builder.addClaimedRewardStages(i + 1); // Stage IDs start from 1
                }
            }
        }
        
        // Stars earned
        if (entity.getStars() != null) {
            builder.setStarsEarned(entity.getStars());
        }
        
        return builder.build();
    }

    private ResponseStatus buildSuccessStatus() {
        return ResponseStatus.newBuilder()
                .setSuccess(true)
                .setCode(200)
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

