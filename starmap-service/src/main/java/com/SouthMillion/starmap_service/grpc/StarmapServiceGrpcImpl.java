package com.SouthMillion.starmap_service.grpc;

import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;
import com.SouthMillion.starmap_service.service.StarMapService;
import org.SouthMillion.proto.starmap.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Starmap Service
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class StarmapServiceGrpcImpl extends StarmapGrpcServiceGrpc.StarmapGrpcServiceImplBase {
    
    private final StarMapService starMapService;
    
    @Override
    public void getAllStars(GetAllStarsRequest request, StreamObserver<GetAllStarsResponse> responseObserver) {
        try {
            List<Star> stars = starMapService.getAllStars(request.getUserId());
            List<StarData> starDataList = stars.stream()
                .map(this::toStarData)
                .collect(Collectors.toList());
            
            GetAllStarsResponse response = GetAllStarsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .addAllStars(starDataList)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getAllStars gRPC call", e);
            responseObserver.onNext(GetAllStarsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getStar(GetStarRequest request, StreamObserver<GetStarResponse> responseObserver) {
        try {
            Star star = starMapService.getStar(request.getUserId(), request.getStarId());
            
            GetStarResponse response = GetStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .setStar(toStarData(star))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getStar gRPC call", e);
            responseObserver.onNext(GetStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void activateStar(ActivateStarRequest request, StreamObserver<ActivateStarResponse> responseObserver) {
        try {
            Star star = starMapService.activateStar(request.getUserId(), request.getStarId());
            
            ActivateStarResponse response = ActivateStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Star activated successfully")
                    .setSuccess(true)
                    .build())
                .setStar(toStarData(star))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in activateStar gRPC call", e);
            responseObserver.onNext(ActivateStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void levelUpStar(LevelUpStarRequest request, StreamObserver<LevelUpStarResponse> responseObserver) {
        try {
            Star star = starMapService.levelUpStar(request.getUserId(), request.getStarId());
            
            LevelUpStarResponse response = LevelUpStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Star leveled up successfully")
                    .setSuccess(true)
                    .build())
                .setStar(toStarData(star))
                .setEnergyCost(1000L)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in levelUpStar gRPC call", e);
            responseObserver.onNext(LevelUpStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getAllConstellations(GetAllConstellationsRequest request, StreamObserver<GetAllConstellationsResponse> responseObserver) {
        try {
            List<Constellation> constellations = starMapService.getAllConstellations(request.getUserId());
            List<ConstellationData> constellationDataList = constellations.stream()
                .map(this::toConstellationData)
                .collect(Collectors.toList());
            
            GetAllConstellationsResponse response = GetAllConstellationsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .addAllConstellations(constellationDataList)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getAllConstellations gRPC call", e);
            responseObserver.onNext(GetAllConstellationsResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getConstellation(GetConstellationRequest request, StreamObserver<GetConstellationResponse> responseObserver) {
        try {
            Constellation constellation = starMapService.getConstellation(
                request.getUserId(), request.getConstellationId());
            
            GetConstellationResponse response = GetConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .setConstellation(toConstellationData(constellation))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getConstellation gRPC call", e);
            responseObserver.onNext(GetConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void unlockConstellation(UnlockConstellationRequest request, StreamObserver<UnlockConstellationResponse> responseObserver) {
        try {
            Constellation constellation = starMapService.unlockConstellation(
                request.getUserId(), request.getConstellationId());
            
            UnlockConstellationResponse response = UnlockConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Constellation unlocked successfully")
                    .setSuccess(true)
                    .build())
                .setConstellation(toConstellationData(constellation))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in unlockConstellation gRPC call", e);
            responseObserver.onNext(UnlockConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void levelUpConstellation(LevelUpConstellationRequest request, StreamObserver<LevelUpConstellationResponse> responseObserver) {
        try {
            Constellation constellation = starMapService.levelUpConstellation(
                request.getUserId(), request.getConstellationId());
            
            LevelUpConstellationResponse response = LevelUpConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Constellation leveled up successfully")
                    .setSuccess(true)
                    .build())
                .setConstellation(toConstellationData(constellation))
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in levelUpConstellation gRPC call", e);
            responseObserver.onNext(LevelUpConstellationResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getTotalStarmapPower(GetTotalStarmapPowerRequest request, StreamObserver<GetTotalStarmapPowerResponse> responseObserver) {
        try {
            Long totalPower = starMapService.calculateTotalStarMapPower(request.getUserId());
            List<Star> stars = starMapService.getAllStars(request.getUserId());
            List<Constellation> constellations = starMapService.getAllConstellations(request.getUserId());
            
            int activatedStars = (int) stars.stream().filter(Star::getIsActive).count();
            int completedConstellations = (int) constellations.stream().filter(Constellation::getIsCompleted).count();
            
            GetTotalStarmapPowerResponse response = GetTotalStarmapPowerResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Success")
                    .setSuccess(true)
                    .build())
                .setTotalPower(totalPower)
                .setTotalStarsActivated(activatedStars)
                .setTotalConstellationsCompleted(completedConstellations)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getTotalStarmapPower gRPC call", e);
            responseObserver.onNext(GetTotalStarmapPowerResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Error: " + e.getMessage())
                    .setSuccess(false)
                    .build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    private StarData toStarData(Star star) {
        return StarData.newBuilder()
            .setId(star.getId())
            .setUserId(star.getUserId())
            .setStarId(star.getStarId())
            .setLevel(star.getLevel())
            .setIsActive(star.getIsActive())
            .setEnergy(star.getEnergy())
            .setCreatedAt(star.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setUpdatedAt(star.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build();
    }
    
    private ConstellationData toConstellationData(Constellation constellation) {
        return ConstellationData.newBuilder()
            .setId(constellation.getId())
            .setUserId(constellation.getUserId())
            .setConstellationId(constellation.getConstellationId())
            .setLevel(constellation.getLevel())
            .setIsUnlocked(constellation.getIsUnlocked())
            .setIsCompleted(constellation.getIsCompleted())
            .setStarsActivated(constellation.getStarsActivated())
            .setTotalStars(constellation.getTotalStars())
            .setPower(constellation.getPower())
            .setCreatedAt(constellation.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setUpdatedAt(constellation.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build();
    }
}

