package com.SouthMillion.angel_service.grpc;

import com.SouthMillion.angel_service.model.entity.Angel;
import com.SouthMillion.angel_service.service.AngelService;
import org.SouthMillion.proto.angel.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Angel Service
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AngelServiceGrpcImpl extends AngelGrpcServiceGrpc.AngelGrpcServiceImplBase {
    
    private final AngelService angelService;
    
    @Override
    public void getUserAngels(GetUserAngelsRequest request, StreamObserver<GetUserAngelsResponse> responseObserver) {
        try {
            log.debug("gRPC getUserAngels called for userId: {}", request.getUserId());
            
            List<Angel> angels = angelService.getAllAngels(request.getUserId());
            List<AngelData> angelDataList = angels.stream()
                .map(this::toAngelData)
                .collect(Collectors.toList());
            boolean hasData = !angelDataList.isEmpty();
            if (!hasData) {
                log.debug("No angel rows found for userId={}", request.getUserId());
            }
            
            GetUserAngelsResponse response = GetUserAngelsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage(hasData ? "Success" : "No angel data")
                    .setSuccess(true)
                    .build())
                .addAllAngels(angelDataList)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in getUserAngels gRPC call", e);
            GetUserAngelsResponse response = GetUserAngelsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getAngel(GetAngelRequest request, StreamObserver<GetAngelResponse> responseObserver) {
        try {
            log.debug("gRPC getAngel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            Angel angel = angelService.getAngel(request.getUserId(), request.getAngelIndex());
            
            GetAngelResponse response = GetAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in getAngel gRPC call", e);
            GetAngelResponse response = GetAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void unlockAngel(UnlockAngelRequest request, StreamObserver<UnlockAngelResponse> responseObserver) {
        try {
            log.info("gRPC unlockAngel called for userId: {}, angelId: {}", 
                request.getUserId(), request.getAngelId());
            
            Angel angel = angelService.unlockAngel(request.getUserId(), request.getAngelId());
            
            UnlockAngelResponse response = UnlockAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel unlocked successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in unlockAngel gRPC call", e);
            UnlockAngelResponse response = UnlockAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void levelUpAngel(LevelUpAngelRequest request, StreamObserver<LevelUpAngelResponse> responseObserver) {
        try {
            log.info("gRPC levelUpAngel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            Angel angel = angelService.levelUpAngel(request.getUserId(), request.getAngelIndex());
            
            LevelUpAngelResponse response = LevelUpAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel leveled up successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .setExpGained(1000L) // Example value
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in levelUpAngel gRPC call", e);
            LevelUpAngelResponse response = LevelUpAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void gradeUpAngel(GradeUpAngelRequest request, StreamObserver<GradeUpAngelResponse> responseObserver) {
        try {
            log.info("gRPC gradeUpAngel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            Angel angel = angelService.gradeUpAngel(request.getUserId(), request.getAngelIndex());
            
            GradeUpAngelResponse response = GradeUpAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel grade upgraded successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in gradeUpAngel gRPC call", e);
            GradeUpAngelResponse response = GradeUpAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void equipAngel(EquipAngelRequest request, StreamObserver<EquipAngelResponse> responseObserver) {
        try {
            log.info("gRPC equipAngel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            angelService.equipAngel(request.getUserId(), request.getAngelIndex());
            
            EquipAngelResponse response = EquipAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel equipped successfully")
                    .setSuccess(true)
                    .build())
                .setMessage("Angel equipped successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in equipAngel gRPC call", e);
            EquipAngelResponse response = EquipAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void unequipAngel(UnequipAngelRequest request, StreamObserver<UnequipAngelResponse> responseObserver) {
        try {
            log.info("gRPC unequipAngel called for userId: {}", request.getUserId());
            
            angelService.unequipAngel(request.getUserId());
            
            UnequipAngelResponse response = UnequipAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel unequipped successfully")
                    .setSuccess(true)
                    .build())
                .setMessage("Angel unequipped successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in unequipAngel gRPC call", e);
            UnequipAngelResponse response = UnequipAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void upgradeStarLevel(UpgradeStarLevelRequest request, StreamObserver<UpgradeStarLevelResponse> responseObserver) {
        try {
            log.info("gRPC upgradeStarLevel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            Angel angel = angelService.upgradeStarLevel(request.getUserId(), request.getAngelIndex());
            
            UpgradeStarLevelResponse response = UpgradeStarLevelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel star level upgraded successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in upgradeStarLevel gRPC call", e);
            UpgradeStarLevelResponse response = UpgradeStarLevelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void upgradeSkill(UpgradeSkillRequest request, StreamObserver<UpgradeSkillResponse> responseObserver) {
        try {
            log.info("gRPC upgradeSkill called for userId: {}, angelIndex: {}, skillSlot: {}", 
                request.getUserId(), request.getAngelIndex(), request.getSkillSlot());
            
            Angel angel = angelService.upgradeSkill(request.getUserId(), 
                request.getAngelIndex(), request.getSkillSlot());
            
            int newSkillLevel = getSkillLevel(angel, request.getSkillSlot());
            
            UpgradeSkillResponse response = UpgradeSkillResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel skill upgraded successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .setNewSkillLevel(newSkillLevel)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in upgradeSkill gRPC call", e);
            UpgradeSkillResponse response = UpgradeSkillResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void evolveAngel(EvolveAngelRequest request, StreamObserver<EvolveAngelResponse> responseObserver) {
        try {
            log.info("gRPC evolveAngel called for userId: {}, angelIndex: {}", 
                request.getUserId(), request.getAngelIndex());
            
            Angel angel = angelService.evolveAngel(request.getUserId(), request.getAngelIndex());
            
            EvolveAngelResponse response = EvolveAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Angel evolved successfully")
                    .setSuccess(true)
                    .build())
                .setAngel(toAngelData(angel))
                .setNewEvolutionStage(angel.getEvolutionStage())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in evolveAngel gRPC call", e);
            EvolveAngelResponse response = EvolveAngelResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    /**
     * Convert Angel entity to AngelData proto message
     */
    @Override
    public void appearanceLevelUp(AppearanceLevelUpRequest request, StreamObserver<AppearanceLevelUpResponse> responseObserver) {
        try {
            Angel angel = angelService.upgradeAppearance(request.getUserId(), request.getAngelIndex(), request.getTargetLevel());
            AppearanceLevelUpResponse response = AppearanceLevelUpResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setAngel(toAngelData(angel))
                .setNewAppearanceLevel(angel.getAppearanceLevel())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in appearanceLevelUp", e);
            responseObserver.onNext(AppearanceLevelUpResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage("Error: " + e.getMessage()).setSuccess(false).build()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void useAppearance(UseAppearanceRequest request, StreamObserver<UseAppearanceResponse> responseObserver) {
        try {
            angelService.setAppearance(request.getUserId(), request.getAngelIndex(), request.getAppearanceId());
            responseObserver.onNext(UseAppearanceResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setMessage("Appearance set successfully").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in useAppearance", e);
            responseObserver.onNext(UseAppearanceResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage("Error: " + e.getMessage()).setSuccess(false).build()).build());
            responseObserver.onCompleted();
        }
    }

    private AngelData toAngelData(Angel angel) {
        long combatPower = angelService.calculateAngelPower(angel);
        
        AngelData.Builder builder = AngelData.newBuilder()
            .setId(angel.getId())
            .setUserId(angel.getUserId())
            .setAngelIndex(angel.getAngelIndex())
            .setAngelId(angel.getAngelId())
            .setLevel(angel.getLevel())
            .setGrade(angel.getGrade())
            .setExp(angel.getExp())
            .setIsActive(angel.getIsActive())
            .setIsEquipped(angel.getIsEquipped())
            .setStarLevel(angel.getStarLevel())
            .setEvolutionStage(angel.getEvolutionStage())
            .setBlessingPoints(angel.getBlessingPoints())
            .setCombatPower(combatPower)
            .setCreatedAt(angel.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setUpdatedAt(angel.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        
        // Skills
        if (angel.getSkill1Id() != null) {
            builder.setSkill1Id(angel.getSkill1Id());
        }
        builder.setSkill1Level(angel.getSkill1Level());
        
        if (angel.getSkill2Id() != null) {
            builder.setSkill2Id(angel.getSkill2Id());
        }
        builder.setSkill2Level(angel.getSkill2Level());
        
        if (angel.getSkill3Id() != null) {
            builder.setSkill3Id(angel.getSkill3Id());
        }
        builder.setSkill3Level(angel.getSkill3Level());
        
        if (angel.getSkill4Id() != null) {
            builder.setSkill4Id(angel.getSkill4Id());
        }
        builder.setSkill4Level(angel.getSkill4Level());
        
        if (angel.getAppearanceId() != null) {
            builder.setAppearanceId(angel.getAppearanceId());
        }
        
        return builder.build();
    }
    
    /**
     * Get skill level by slot number
     */
    private int getSkillLevel(Angel angel, int skillSlot) {
        return switch (skillSlot) {
            case 1 -> angel.getSkill1Level();
            case 2 -> angel.getSkill2Level();
            case 3 -> angel.getSkill3Level();
            case 4 -> angel.getSkill4Level();
            default -> 0;
        };
    }
}

