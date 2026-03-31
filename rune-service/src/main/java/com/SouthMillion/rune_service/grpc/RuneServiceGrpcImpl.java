package com.SouthMillion.rune_service.grpc;

import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.service.RuneService;
import org.SouthMillion.proto.rune.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RuneServiceGrpcImpl extends RuneGrpcServiceGrpc.RuneGrpcServiceImplBase {
    
    private final RuneService runeService;
    
    @Override
    public void getAllRunes(GetAllRunesRequest request, StreamObserver<GetAllRunesResponse> responseObserver) {
        try {
            List<Rune> runes = runeService.getAllRunes(request.getUserId());
            GetAllRunesResponse response = GetAllRunesResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .addAllRunes(runes.stream().map(this::toRuneData).collect(Collectors.toList()))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getAllRunes", e);
            responseObserver.onNext(GetAllRunesResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void createRune(CreateRuneRequest request, StreamObserver<CreateRuneResponse> responseObserver) {
        try {
            Rune rune = runeService.createRune(request.getUserId(), request.getRuneId(), request.getQuality());
            CreateRuneResponse response = CreateRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setRune(toRuneData(rune))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in createRune", e);
            responseObserver.onNext(CreateRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void equipRune(EquipRuneRequest request, StreamObserver<EquipRuneResponse> responseObserver) {
        try {
            Rune rune = runeService.equipRune(request.getUserId(), request.getRuneIndex(), request.getEquipSlot());
            EquipRuneResponse response = EquipRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .setRune(toRuneData(rune))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in equipRune", e);
            responseObserver.onNext(EquipRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getEquippedRunes(GetEquippedRunesRequest request, StreamObserver<GetEquippedRunesResponse> responseObserver) {
        try {
            List<Rune> runes = runeService.getEquippedRunes(request.getUserId());
            GetEquippedRunesResponse response = GetEquippedRunesResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                .addAllRunes(runes.stream().map(this::toRuneData).collect(Collectors.toList()))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getEquippedRunes", e);
            responseObserver.onNext(GetEquippedRunesResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(500).setMessage(e.getMessage()).setSuccess(false).build())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    private RuneData toRuneData(Rune rune) {
        return RuneData.newBuilder()
            .setId(rune.getId())
            .setUserId(rune.getUserId())
            .setRuneIndex(rune.getRuneIndex())
            .setRuneId(rune.getRuneId())
            .setLevel(rune.getLevel())
            .setQuality(rune.getQuality())
            .setStarLevel(rune.getStar())
            .setExp(rune.getExp())
            .setIsEquipped(rune.getIsEquipped())
            .setEquipSlot(rune.getEquipSlot() != null ? rune.getEquipSlot() : 0)
            .setRefinementLevel(rune.getRefinementLevel())
            .setCombatPower(0L)  // Calculate from attributes if needed
            .setCreatedAt(rune.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setUpdatedAt(rune.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build();
    }
}

