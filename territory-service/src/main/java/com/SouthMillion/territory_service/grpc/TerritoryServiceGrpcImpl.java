package com.SouthMillion.territory_service.grpc;

import com.SouthMillion.territory_service.model.entity.Territory;
import com.SouthMillion.territory_service.service.TerritoryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.territory.*;

/**
 * gRPC Server Implementation for Territory Service (领地系统)
 * Uses protoc-generated classes from common-lib territory_service.proto
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TerritoryServiceGrpcImpl extends TerritoryServiceGrpc.TerritoryServiceImplBase {

    private final TerritoryService territoryService;

    @Override
    public void getTerritoryInfo(TerritoryRequest req,
                                 StreamObserver<TerritoryInfoResponse> obs) {
        try {
            Territory t = territoryService.getTerritory(req.getRoleId());
            TerritoryInfoResponse.Builder b =
                    TerritoryInfoResponse.newBuilder().setStatus(ok());
            if (t != null) {
                b.setTerritoryLevel(t.getLevel() != null ? t.getLevel() : 0)
                 .setBotNum(0).setBotRunNum(0).setBotBuyCount(0).setRewardCount(0).setReason(0);
            }
            obs.onNext(b.build());
            obs.onCompleted();
            log.info("[grpc-territory] GetTerritoryInfo roleId={}", req.getRoleId());
        } catch (Exception e) {
            log.error("[grpc-territory] GetTerritoryInfo error: {}", e.getMessage(), e);
            obs.onNext(TerritoryInfoResponse.newBuilder().setStatus(err(e)).build());
            obs.onCompleted();
        }
    }

    @Override
    public void getNeighbourInfo(TerritoryRequest req,
                                 StreamObserver<TerritoryNeighbourResponse> obs) {
        try {
            obs.onNext(TerritoryNeighbourResponse.newBuilder()
                    .setNeighbourTime(0).setStatus(ok()).build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onNext(TerritoryNeighbourResponse.newBuilder().setStatus(err(e)).build());
            obs.onCompleted();
        }
    }

    @Override
    public void getBotInfo(TerritoryRequest req,
                           StreamObserver<TerritoryBotResponse> obs) {
        obs.onNext(TerritoryBotResponse.newBuilder().setStatus(ok()).build());
        obs.onCompleted();
    }

    @Override
    public void getReportList(TerritoryRequest req,
                              StreamObserver<TerritoryReportResponse> obs) {
        obs.onNext(TerritoryReportResponse.newBuilder().setStatus(ok()).build());
        obs.onCompleted();
    }

    @Override
    public void getRedInfo(TerritoryRequest req,
                           StreamObserver<TerritoryRedResponse> obs) {
        obs.onNext(TerritoryRedResponse.newBuilder()
                .setRewardFlag(0).setStatus(ok()).build());
        obs.onCompleted();
    }

    @Override
    public void dispatchAction(TerritoryActionRequest req,
                               StreamObserver<TerritoryActionResponse> obs) {
        try {
            int type = req.getType();
            long roleId = req.getRoleId();
            switch (type) {
                case 1 -> territoryService.collectResources(roleId);
                case 2 -> territoryService.levelUpTerritory(roleId);
                case 3 -> log.info("[grpc-territory] BuyBot roleId={} (accepted)", roleId); // bot purchase accepted
                default -> log.warn("[grpc-territory] Unknown action type={}", type);
            }
            obs.onNext(TerritoryActionResponse.newBuilder()
                    .setSuccess(true).setResultType(type).setStatus(ok()).build());
            obs.onCompleted();
            log.info("[grpc-territory] DispatchAction roleId={} type={}", roleId, type);
        } catch (Exception e) {
            log.error("[grpc-territory] DispatchAction error: {}", e.getMessage(), e);
            obs.onNext(TerritoryActionResponse.newBuilder()
                    .setSuccess(false).setStatus(err(e)).build());
            obs.onCompleted();
        }
    }

    private ResponseStatus ok() {
        return ResponseStatus.newBuilder().setSuccess(true).build();
    }

    private ResponseStatus err(Exception e) {
        return ResponseStatus.newBuilder()
                .setSuccess(false)
                .setMessage(e.getMessage() != null ? e.getMessage() : "Internal error")
                .build();
    }
}
