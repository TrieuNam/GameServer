package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.territory.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for territory-service (领地系统)
 *
 * territory-service gRPC port: 9086
 * webSocket-server connects via: discovery:///territory-service
 */
@Slf4j
@Service
public class TerritoryGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TerritoryGrpcClient.class);

    @GrpcClient("territory-service")
    private TerritoryServiceGrpc.TerritoryServiceBlockingStub stub;

    private static boolean isUnavailable(StatusRuntimeException e) {
        return e.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    // ─── GetTerritoryInfo ────────────────────────────────────────────────
    public TerritoryInfoResponse getTerritoryInfo(Long roleId) {
        try {
            TerritoryInfoResponse resp = stub.getTerritoryInfo(
                    TerritoryRequest.newBuilder().setRoleId(roleId).build());
            log.debug("[grpc-territory] getTerritoryInfo roleId={}", roleId);
            return resp;
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] getTerritoryInfo: territory-service unavailable");
            else log.error("[grpc-territory] getTerritoryInfo error: {}", e.getMessage());
            return TerritoryInfoResponse.getDefaultInstance();
        }
    }

    // ─── GetNeighbourInfo ────────────────────────────────────────────────
    public TerritoryNeighbourResponse getNeighbourInfo(Long roleId) {
        try {
            return stub.getNeighbourInfo(TerritoryRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] getNeighbourInfo: territory-service unavailable");
            else log.error("[grpc-territory] getNeighbourInfo error: {}", e.getMessage());
            return TerritoryNeighbourResponse.getDefaultInstance();
        }
    }

    // ─── GetBotInfo ──────────────────────────────────────────────────────
    public TerritoryBotResponse getBotInfo(Long roleId) {
        try {
            return stub.getBotInfo(TerritoryRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] getBotInfo: territory-service unavailable");
            else log.error("[grpc-territory] getBotInfo error: {}", e.getMessage());
            return TerritoryBotResponse.getDefaultInstance();
        }
    }

    // ─── GetReportList ───────────────────────────────────────────────────
    public TerritoryReportResponse getReportList(Long roleId) {
        try {
            return stub.getReportList(TerritoryRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] getReportList: territory-service unavailable");
            else log.error("[grpc-territory] getReportList error: {}", e.getMessage());
            return TerritoryReportResponse.getDefaultInstance();
        }
    }

    // ─── GetRedInfo ──────────────────────────────────────────────────────
    public TerritoryRedResponse getRedInfo(Long roleId) {
        try {
            return stub.getRedInfo(TerritoryRequest.newBuilder().setRoleId(roleId).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] getRedInfo: territory-service unavailable");
            else log.error("[grpc-territory] getRedInfo error: {}", e.getMessage());
            return TerritoryRedResponse.getDefaultInstance();
        }
    }

    // ─── DispatchAction ──────────────────────────────────────────────────
    public TerritoryActionResponse dispatchAction(Long roleId, int type) {
        try {
            return stub.dispatchAction(
                    TerritoryActionRequest.newBuilder().setRoleId(roleId).setType(type).build());
        } catch (StatusRuntimeException e) {
            if (isUnavailable(e)) log.warn("[grpc-territory] dispatchAction: territory-service unavailable");
            else log.error("[grpc-territory] dispatchAction error: {}", e.getMessage());
            return TerritoryActionResponse.newBuilder().setSuccess(false).build();
        }
    }
}
