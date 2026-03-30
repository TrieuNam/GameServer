package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.escort.*;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for escort-service
 * Uses protoc-generated classes from common-lib escort_service.proto
 */
@Slf4j
@Service
public class EscortGrpcClient {

    @GrpcClient("escort-service")
    private EscortServiceGrpc.EscortServiceBlockingStub stub;

    private static boolean isUnavailable(StatusRuntimeException e) {
        return e.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public EscortInfoResponse getEscortInfo(Long roleId) {
        try {
            EscortInfoResponse resp = stub.getEscortInfo(
                    EscortInfoRequest.newBuilder().setRoleId(roleId).build());
            log.debug("[grpc-escort] getEscortInfo roleId={}", roleId);
            return resp;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] getEscortInfo: escort-service unavailable");
            else log.error("[grpc-escort] getEscortInfo error: {}", e.getMessage());
            return EscortInfoResponse.getDefaultInstance();
        }
    }

    public EscortShipListResponse getShipList(Long roleId) {
        try {
            return stub.getShipList(EscortInfoRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] getShipList: escort-service unavailable");
            else log.error("[grpc-escort] getShipList error: {}", e.getMessage());
            return EscortShipListResponse.getDefaultInstance();
        }
    }

    public EscortActionResponse startEscort(Long roleId, int shipLevel) {
        try {
            return stub.startEscort(StartEscortRequest.newBuilder()
                    .setRoleId(roleId).setShipLevel(shipLevel).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] startEscort: escort-service unavailable");
            else log.error("[grpc-escort] startEscort error: {}", e.getMessage());
            return EscortActionResponse.getDefaultInstance();
        }
    }

    public EscortActionResponse interceptEscort(Long roleId, int shipKey, long targetUid) {
        try {
            return stub.interceptEscort(InterceptRequest.newBuilder()
                    .setRoleId(roleId).setShipKey(shipKey).setTargetUid(targetUid).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] interceptEscort: escort-service unavailable");
            else log.error("[grpc-escort] interceptEscort error: {}", e.getMessage());
            return EscortActionResponse.getDefaultInstance();
        }
    }

    public EscortActionResponse helpEscort(Long roleId, int shipKey, long targetUid) {
        try {
            return stub.helpEscort(HelpRequest.newBuilder()
                    .setRoleId(roleId).setShipKey(shipKey).setTargetUid(targetUid).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] helpEscort: escort-service unavailable");
            else log.error("[grpc-escort] helpEscort error: {}", e.getMessage());
            return EscortActionResponse.getDefaultInstance();
        }
    }

    public EscortActionResponse claimReward(Long roleId) {
        try {
            return stub.claimReward(EscortInfoRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] claimReward: escort-service unavailable");
            else log.error("[grpc-escort] claimReward error: {}", e.getMessage());
            return EscortActionResponse.getDefaultInstance();
        }
    }

    public EscortReportListResponse getReportList(Long roleId) {
        try {
            return stub.getReportList(EscortInfoRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] getReportList: escort-service unavailable");
            else log.error("[grpc-escort] getReportList error: {}", e.getMessage());
            return EscortReportListResponse.getDefaultInstance();
        }
    }

    public EscortInterceptListResponse getInterceptList(Long roleId) {
        try {
            return stub.getInterceptList(EscortInfoRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-escort] getInterceptList: escort-service unavailable");
            else log.error("[grpc-escort] getInterceptList error: {}", e.getMessage());
            return EscortInterceptListResponse.getDefaultInstance();
        }
    }
}
