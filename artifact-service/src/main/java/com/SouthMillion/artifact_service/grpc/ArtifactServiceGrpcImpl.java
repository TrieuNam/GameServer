package com.SouthMillion.artifact_service.grpc;

import com.SouthMillion.artifact_service.model.entity.Artifact;
import com.SouthMillion.artifact_service.service.ArtifactService;
import org.SouthMillion.proto.artifact.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ArtifactServiceGrpcImpl extends ArtifactGrpcServiceGrpc.ArtifactGrpcServiceImplBase {
    
    private final ArtifactService artifactService;
    
    @Override
    public void getAllArtifacts(GetAllArtifactsRequest request, StreamObserver<GetAllArtifactsResponse> responseObserver) {
        try {
            List<Artifact> artifacts = artifactService.getAllArtifacts(request.getUserId());
            GetAllArtifactsResponse response = GetAllArtifactsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .addAllArtifacts(artifacts.stream().map(this::toArtifactData).collect(Collectors.toList()))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getAllArtifacts", e);
            responseObserver.onNext(GetAllArtifactsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getArtifact(GetArtifactRequest request, StreamObserver<GetArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.getArtifact(request.getUserId(), request.getArtifactIndex());
            responseObserver.onNext(GetArtifactResponse.newBuilder()
                .setStatus(ok())
                .setArtifact(toArtifactData(artifact))
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getArtifact", e);
            responseObserver.onNext(GetArtifactResponse.newBuilder()
                .setStatus(error(e))
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void unlockArtifact(UnlockArtifactRequest request, StreamObserver<UnlockArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.unlockArtifact(request.getUserId(), request.getArtifactId());
            UnlockArtifactResponse response = UnlockArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setArtifact(toArtifactData(artifact))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in unlockArtifact", e);
            responseObserver.onNext(UnlockArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void levelUpArtifact(LevelUpArtifactRequest request, StreamObserver<LevelUpArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.levelUpArtifact(request.getUserId(), request.getArtifactIndex());
            LevelUpArtifactResponse response = LevelUpArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setArtifact(toArtifactData(artifact))
                .setExpGained(1000L)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in levelUpArtifact", e);
            responseObserver.onNext(LevelUpArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void gradeUpArtifact(GradeUpArtifactRequest request, StreamObserver<GradeUpArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.gradeUpArtifact(request.getUserId(), request.getArtifactIndex());
            responseObserver.onNext(GradeUpArtifactResponse.newBuilder()
                .setStatus(ok())
                .setArtifact(toArtifactData(artifact))
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gradeUpArtifact", e);
            responseObserver.onNext(GradeUpArtifactResponse.newBuilder()
                .setStatus(error(e))
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void equipArtifact(EquipArtifactRequest request, StreamObserver<EquipArtifactResponse> responseObserver) {
        try {
            artifactService.equipArtifact(request.getUserId(), request.getArtifactIndex());
            responseObserver.onNext(EquipArtifactResponse.newBuilder()
                .setStatus(ok())
                .setMessage("equipped")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in equipArtifact", e);
            responseObserver.onNext(EquipArtifactResponse.newBuilder()
                .setStatus(error(e))
                .setMessage(e.getMessage())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unequipArtifact(UnequipArtifactRequest request, StreamObserver<UnequipArtifactResponse> responseObserver) {
        try {
            artifactService.unequipArtifact(request.getUserId());
            responseObserver.onNext(UnequipArtifactResponse.newBuilder()
                .setStatus(ok())
                .setMessage("unequipped")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in unequipArtifact", e);
            responseObserver.onNext(UnequipArtifactResponse.newBuilder()
                .setStatus(error(e))
                .setMessage(e.getMessage())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void refineArtifact(RefineArtifactRequest request, StreamObserver<RefineArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.refineArtifact(request.getUserId(), request.getArtifactIndex());
            responseObserver.onNext(RefineArtifactResponse.newBuilder()
                .setStatus(ok())
                .setArtifact(toArtifactData(artifact))
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in refineArtifact", e);
            responseObserver.onNext(RefineArtifactResponse.newBuilder()
                .setStatus(error(e))
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void awakenArtifact(AwakenArtifactRequest request, StreamObserver<AwakenArtifactResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.awakenArtifact(request.getUserId(), request.getArtifactIndex());
            AwakenArtifactResponse response = AwakenArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setArtifact(toArtifactData(artifact))
                .setNewAwakeningStage(artifact.getAwakeningStage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in awakenArtifact", e);
            responseObserver.onNext(AwakenArtifactResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void upgradeArtifactSkill(UpgradeArtifactSkillRequest request, StreamObserver<UpgradeArtifactSkillResponse> responseObserver) {
        try {
            Artifact artifact = artifactService.upgradeArtifactSkill(
                request.getUserId(),
                request.getArtifactIndex(),
                request.getSkillIndex());
            int newSkillLevel = switch (request.getSkillIndex()) {
                case 0 -> artifact.getSkill1Level();
                case 1 -> artifact.getSkill2Level();
                default -> artifact.getSkill3Level();
            };
            responseObserver.onNext(UpgradeArtifactSkillResponse.newBuilder()
                .setStatus(ok())
                .setArtifact(toArtifactData(artifact))
                .setSkillIndex(request.getSkillIndex())
                .setNewSkillLevel(newSkillLevel)
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in upgradeArtifactSkill", e);
            responseObserver.onNext(UpgradeArtifactSkillResponse.newBuilder()
                .setStatus(error(e))
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void drawArtifacts(DrawArtifactsRequest request, StreamObserver<DrawArtifactsResponse> responseObserver) {
        try {
            List<Map<String, Object>> results = artifactService.drawArtifacts(request.getUserId(), request.getDrawType());
            DrawArtifactsResponse.Builder builder = DrawArtifactsResponse.newBuilder().setStatus(ok());
            for (Map<String, Object> result : results) {
                builder.addResults(DrawArtifactResult.newBuilder()
                    .setArtifactId(getInt(result, "artifactId", "artifact_id"))
                    .setQuality(getInt(result, "quality"))
                    .setIsGuaranteed(getBoolean(result, "isGuaranteed", "is_guaranteed"))
                    .build());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in drawArtifacts", e);
            responseObserver.onNext(DrawArtifactsResponse.newBuilder()
                .setStatus(error(e))
                .build());
            responseObserver.onCompleted();
        }
    }

    private ResponseStatus ok() {
        return ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build();
    }

    private ResponseStatus error(Exception e) {
        return ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage() == null ? "Internal error" : e.getMessage()).setSuccess(false).build();
    }

    private int getInt(Map<String, Object> map, String... keys) {
        if (map == null) return 0;
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                try {
                    return Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException ignore) {
                    // continue
                }
            }
        }
        return 0;
    }

    private boolean getBoolean(Map<String, Object> map, String... keys) {
        if (map == null) return false;
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value != null) {
                return Boolean.parseBoolean(String.valueOf(value));
            }
        }
        return false;
    }
    
    private ArtifactData toArtifactData(Artifact artifact) {
        return ArtifactData.newBuilder()
            .setId(artifact.getId())
            .setUserId(artifact.getUserId())
            .setArtifactIndex(artifact.getArtifactIndex())
            .setArtifactId(artifact.getArtifactId())
            .setLevel(artifact.getLevel())
            .setGrade(artifact.getGrade())
            .setExp(artifact.getExp())
            .setIsActive(artifact.getIsActive())
            .setIsEquipped(artifact.getIsEquipped())
            .setRefinementLevel(artifact.getRefinementLevel())
            .setAwakeningStage(artifact.getAwakeningStage())
            .setSoulPower(artifact.getSoulPower())
            .setDivineEssence(artifact.getDivineEssence())
            .setBlessingLevel(artifact.getBlessingTier())
            .setCombatPower(artifactService.calculateArtifactPower(artifact))
            .setCreatedAt(artifact.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setUpdatedAt(artifact.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build();
    }
}

