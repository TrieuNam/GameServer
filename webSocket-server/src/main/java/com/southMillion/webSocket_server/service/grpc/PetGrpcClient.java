package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.pet.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PetGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PetGrpcClient.class);

    @GrpcClient("pet-service")
    private PetServiceGrpc.PetServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetRolePetsResponse getRolePets(long roleId) {
        try {
            return stub.getRolePets(GetRolePetsRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pet] getRolePets: pet-service unavailable");
            else log.error("[grpc-pet] getRolePets error: {}", e.getMessage());
            return GetRolePetsResponse.newBuilder().setSuccess(false).build();
        }
    }

    public PetResponse activatePet(long roleId, int petTemplateId) {
        try {
            return stub.activatePet(ActivatePetRequest.newBuilder().setRoleId(roleId).setPetTemplateId(petTemplateId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pet] activatePet: pet-service unavailable");
            else log.error("[grpc-pet] activatePet error: {}", e.getMessage());
            return PetResponse.newBuilder().setSuccess(false).build();
        }
    }

    public PetResponse upgradePet(long roleId, long petId, List<Long> materialIds) {
        try {
            return stub.upgradePet(UpgradePetRequest.newBuilder()
                    .setRoleId(roleId).setPetId(petId).addAllMaterialIds(materialIds).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pet] upgradePet: pet-service unavailable");
            else log.error("[grpc-pet] upgradePet error: {}", e.getMessage());
            return PetResponse.newBuilder().setSuccess(false).build();
        }
    }

    public PetResponse evolvePet(long roleId, long petId) {
        try {
            return stub.evolvePet(EvolvePetRequest.newBuilder().setRoleId(roleId).setPetId(petId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pet] evolvePet: pet-service unavailable");
            else log.error("[grpc-pet] evolvePet error: {}", e.getMessage());
            return PetResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus setActivePet(long roleId, long petId) {
        try {
            return stub.setActivePet(SetActivePetRequest.newBuilder().setRoleId(roleId).setPetId(petId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pet] setActivePet: pet-service unavailable");
            else log.error("[grpc-pet] setActivePet error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }
}
