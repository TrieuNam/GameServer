package com.SouthMillion.pet_service.grpc;

import com.SouthMillion.pet_service.model.dto.PetAllInfoResponse;
import com.SouthMillion.pet_service.model.dto.PetDataDTO;
import com.SouthMillion.pet_service.model.entity.Pet;
import com.SouthMillion.pet_service.service.PetService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.pet.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PetServiceGrpcImpl extends PetServiceGrpc.PetServiceImplBase {

    private final PetService petService;

    @Override
    public void getRolePets(GetRolePetsRequest request, StreamObserver<GetRolePetsResponse> responseObserver) {
        log.info("[PetGrpc] GetRolePets: roleId={}", request.getRoleId());
        try {
            String userId = String.valueOf(request.getRoleId());
            PetAllInfoResponse info = petService.getAllPetInfo(userId);
            GetRolePetsResponse.Builder resp = GetRolePetsResponse.newBuilder().setSuccess(true).setMessage("OK");
            if (info != null && info.getPetList() != null) {
                for (PetDataDTO p : info.getPetList()) {
                    resp.addPets(toPetDataFromDTO(p));
                }
            }
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] GetRolePets error", e);
            responseObserver.onNext(GetRolePetsResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPet(GetPetRequest request, StreamObserver<PetResponse> responseObserver) {
        log.info("[PetGrpc] GetPet: roleId={}, petId={}", request.getRoleId(), request.getPetId());
        try {
            String userId = String.valueOf(request.getRoleId());
            Pet pet = petService.getPet(userId, (int) request.getPetId());
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(true).setMessage("OK")
                    .setPet(toPetData(pet)).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] GetPet error", e);
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void activatePet(ActivatePetRequest request, StreamObserver<PetResponse> responseObserver) {
        log.info("[PetGrpc] ActivatePet: roleId={}, templateId={}", request.getRoleId(), request.getPetTemplateId());
        try {
            String userId = String.valueOf(request.getRoleId());
            Pet pet = petService.addPet(userId, request.getPetTemplateId());
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(true).setMessage("Pet activated")
                    .setPet(toPetData(pet)).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] ActivatePet error", e);
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void upgradePet(UpgradePetRequest request, StreamObserver<PetResponse> responseObserver) {
        log.info("[PetGrpc] UpgradePet: roleId={}, petId={}", request.getRoleId(), request.getPetId());
        try {
            String userId = String.valueOf(request.getRoleId());
            petService.levelUp(userId, (int) request.getPetId(), 1);
            Pet pet = petService.getPet(userId, (int) request.getPetId());
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(true).setMessage("Pet upgraded")
                    .setPet(toPetData(pet)).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] UpgradePet error", e);
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void evolvePet(EvolvePetRequest request, StreamObserver<PetResponse> responseObserver) {
        log.info("[PetGrpc] EvolvePet: roleId={}, petId={}", request.getRoleId(), request.getPetId());
        try {
            String userId = String.valueOf(request.getRoleId());
            petService.evolve(userId, (int) request.getPetId());
            Pet pet = petService.getPet(userId, (int) request.getPetId());
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(true).setMessage("Pet evolved")
                    .setPet(toPetData(pet)).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] EvolvePet error", e);
            responseObserver.onNext(PetResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void setActivePet(SetActivePetRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.info("[PetGrpc] SetActivePet: roleId={}, petId={}", request.getRoleId(), request.getPetId());
        try {
            String userId = String.valueOf(request.getRoleId());
            petService.setFightPet(userId, (int) request.getPetId(), 0);
            responseObserver.onNext(ResponseStatus.newBuilder().setSuccess(true).setMessage("Active pet set").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[PetGrpc] SetActivePet error", e);
            responseObserver.onNext(ResponseStatus.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void renamePet(RenamePetRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.info("[PetGrpc] RenamePet: roleId={}, petId={}", request.getRoleId(), request.getPetId());
        // Pet entity does not have a name field; return success as no-op
        responseObserver.onNext(ResponseStatus.newBuilder().setSuccess(true).setMessage("OK").build());
        responseObserver.onCompleted();
    }

    private PetData toPetData(Pet p) {
        if (p == null) return PetData.getDefaultInstance();
        return PetData.newBuilder()
                .setPetId(p.getPetIndex() != null ? p.getPetIndex() : 0)
                .setPetTemplateId(p.getPetId() != null ? p.getPetId() : 0)
                .setLevel(p.getLevel() != null ? p.getLevel() : 1)
                .setPetExp(p.getExp() != null ? p.getExp().intValue() : 0)
                .build();
    }

    private PetData toPetDataFromDTO(PetDataDTO p) {
        if (p == null) return PetData.getDefaultInstance();
        return PetData.newBuilder()
                .setPetId(p.getPetIndex() != null ? p.getPetIndex() : 0)
                .setPetTemplateId(p.getPetId() != null ? p.getPetId() : 0)
                .setLevel(p.getPetLevel() != null ? p.getPetLevel() : 1)
                .setPetExp(p.getPetExp() != null ? p.getPetExp().intValue() : 0)
                .build();
    }
}
