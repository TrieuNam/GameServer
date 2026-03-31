package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.starmap.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StarMapGrpcClient {

    @GrpcClient("starmap-service")
    private StarmapGrpcServiceGrpc.StarmapGrpcServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetAllStarsResponse getAllStars(long userId) {
        try {
            return stub.getAllStars(GetAllStarsRequest.newBuilder().setUserId(userId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-starmap] getAllStars userId={}: starmap-service unavailable", userId);
            else log.error("[grpc-starmap] getAllStars userId={} error: {}", userId, e.getMessage());
            return GetAllStarsResponse.getDefaultInstance();
        }
    }

    public ActivateStarResponse activateStar(long userId, int starId) {
        try {
            return stub.activateStar(ActivateStarRequest.newBuilder()
                    .setUserId(userId).setStarId(starId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-starmap] activateStar: starmap-service unavailable");
            else log.error("[grpc-starmap] activateStar userId={} starId={} error: {}", userId, starId, e.getMessage());
            return ActivateStarResponse.getDefaultInstance();
        }
    }

    public LevelUpStarResponse levelUpStar(long userId, int starId) {
        try {
            return stub.levelUpStar(LevelUpStarRequest.newBuilder()
                    .setUserId(userId).setStarId(starId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-starmap] levelUpStar: starmap-service unavailable");
            else log.error("[grpc-starmap] levelUpStar error: {}", e.getMessage());
            return LevelUpStarResponse.getDefaultInstance();
        }
    }

    public GetAllConstellationsResponse getAllConstellations(long userId) {
        try {
            return stub.getAllConstellations(GetAllConstellationsRequest.newBuilder()
                    .setUserId(userId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-starmap] getAllConstellations userId={}: starmap-service unavailable", userId);
            else log.error("[grpc-starmap] getAllConstellations error: {}", e.getMessage());
            return GetAllConstellationsResponse.getDefaultInstance();
        }
    }

    public GetTotalStarmapPowerResponse getTotalPower(long userId) {
        try {
            return stub.getTotalStarmapPower(GetTotalStarmapPowerRequest.newBuilder()
                    .setUserId(userId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-starmap] getTotalPower: starmap-service unavailable");
            else log.error("[grpc-starmap] getTotalPower error: {}", e.getMessage());
            return GetTotalStarmapPowerResponse.getDefaultInstance();
        }
    }
}
