package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.activity.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActivityGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ActivityGrpcClient.class);

    @GrpcClient("activity-service")
    private ActivityServiceGrpc.ActivityServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GenericResponse handleRandActivity(long roleId, int activityType, String op, String paramsJson) {
        try {
            return stub.handleRandActivity(RandActivityRequest.newBuilder()
                    .setRoleId(roleId)
                    .setActivityType(activityType)
                    .setOp(op != null ? op : "")
                    .setParamsJson(paramsJson != null ? paramsJson : "")
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-activity] handleRandActivity: activity-service unavailable");
            else log.error("[grpc-activity] handleRandActivity error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse getSevenDaySign(long roleId) {
        try {
            return stub.getSevenDaySign(RoleRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-activity] getSevenDaySign: activity-service unavailable");
            else log.error("[grpc-activity] getSevenDaySign error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse claimSevenDaySign(long roleId, int day) {
        try {
            return stub.claimSevenDaySign(SignClaimRequest.newBuilder().setRoleId(roleId).setDay(day).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-activity] claimSevenDaySign: activity-service unavailable");
            else log.error("[grpc-activity] claimSevenDaySign error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse getLuckUnpacking(long roleId) {
        try {
            return stub.getLuckUnpacking(RoleRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-activity] getLuckUnpacking: activity-service unavailable");
            else log.error("[grpc-activity] getLuckUnpacking error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse getActivityData(long roleId, int activityType) {
        try {
            return stub.getActivityData(ActivityDataRequest.newBuilder().setRoleId(roleId).setActivityType(activityType).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-activity] getActivityData: activity-service unavailable");
            else log.error("[grpc-activity] getActivityData error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }
}
