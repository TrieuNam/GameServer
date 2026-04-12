package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.angel.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AngelGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AngelGrpcClient.class);

    @GrpcClient("angel-service")
    private AngelGrpcServiceGrpc.AngelGrpcServiceBlockingStub stub;

    private long parseUserId(String userId) {
        try { return Long.parseLong(userId); }
        catch (NumberFormatException e) {
            log.warn("[grpc-angel] userId '{}' is not numeric (ULID) — returning empty", userId);
            return -1L;
        }
    }

    /** UNAVAILABLE = service not in Eureka (down/not-deployed). Expected during dev. */
    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s &&
                s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetUserAngelsResponse getUserAngels(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return GetUserAngelsResponse.getDefaultInstance();
        try { return stub.getUserAngels(GetUserAngelsRequest.newBuilder().setUserId(id).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] getUserAngels: angel-service unavailable");
            else log.error("[grpc-angel] getUserAngels error: {}", e.getMessage());
            return GetUserAngelsResponse.getDefaultInstance();
        }
    }

    public LevelUpAngelResponse levelUpAngel(String userId, int angelIndex) {
        long id = parseUserId(userId);
        if (id < 0) return LevelUpAngelResponse.getDefaultInstance();
        try { return stub.levelUpAngel(LevelUpAngelRequest.newBuilder().setUserId(id).setAngelIndex(angelIndex).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] levelUpAngel: angel-service unavailable");
            else log.error("[grpc-angel] levelUpAngel error: {}", e.getMessage());
            return LevelUpAngelResponse.getDefaultInstance();
        }
    }

    public GradeUpAngelResponse gradeUpAngel(String userId, int angelIndex) {
        long id = parseUserId(userId);
        if (id < 0) return GradeUpAngelResponse.getDefaultInstance();
        try { return stub.gradeUpAngel(GradeUpAngelRequest.newBuilder().setUserId(id).setAngelIndex(angelIndex).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] gradeUpAngel: angel-service unavailable");
            else log.error("[grpc-angel] gradeUpAngel error: {}", e.getMessage());
            return GradeUpAngelResponse.getDefaultInstance();
        }
    }

    public EquipAngelResponse equipAngel(String userId, int angelIndex) {
        long id = parseUserId(userId);
        if (id < 0) return EquipAngelResponse.getDefaultInstance();
        try { return stub.equipAngel(EquipAngelRequest.newBuilder().setUserId(id).setAngelIndex(angelIndex).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] equipAngel: angel-service unavailable");
            else log.error("[grpc-angel] equipAngel error: {}", e.getMessage());
            return EquipAngelResponse.getDefaultInstance();
        }
    }

    public AppearanceLevelUpResponse appearanceLevelUp(String userId, int angelIndex, int targetLevel) {
        long id = parseUserId(userId);
        if (id < 0) return AppearanceLevelUpResponse.getDefaultInstance();
        try { return stub.appearanceLevelUp(AppearanceLevelUpRequest.newBuilder().setUserId(id).setAngelIndex(angelIndex).setTargetLevel(targetLevel).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] appearanceLevelUp: angel-service unavailable");
            else log.error("[grpc-angel] appearanceLevelUp error: {}", e.getMessage());
            return AppearanceLevelUpResponse.getDefaultInstance();
        }
    }

    public UseAppearanceResponse useAppearance(String userId, int angelIndex, int appearanceId) {
        long id = parseUserId(userId);
        if (id < 0) return UseAppearanceResponse.getDefaultInstance();
        try { return stub.useAppearance(UseAppearanceRequest.newBuilder().setUserId(id).setAngelIndex(angelIndex).setAppearanceId(appearanceId).build()); }
        catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-angel] useAppearance: angel-service unavailable");
            else log.error("[grpc-angel] useAppearance error: {}", e.getMessage());
            return UseAppearanceResponse.getDefaultInstance();
        }
    }
}
