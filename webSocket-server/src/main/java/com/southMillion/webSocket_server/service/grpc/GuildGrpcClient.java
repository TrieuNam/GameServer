package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.guild.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GuildGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuildGrpcClient.class);

    @GrpcClient("guild-service")
    private GuildServiceGrpc.GuildServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GuildInfoResponse getMemberGuild(String roleId) {
        try {
            return stub.getMemberGuild(GetMemberGuildRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] getMemberGuild: guild-service unavailable");
            else log.error("[grpc-guild] getMemberGuild error: {}", e.getMessage());
            return GuildInfoResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GuildInfoResponse getGuildInfo(long guildId) {
        try {
            return stub.getGuildInfo(GetGuildInfoRequest.newBuilder().setGuildId(guildId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] getGuildInfo: guild-service unavailable");
            else log.error("[grpc-guild] getGuildInfo error: {}", e.getMessage());
            return GuildInfoResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GuildInfoResponse createGuild(String leaderId, String name, String notice) {
        try {
            return stub.createGuild(CreateGuildRequest.newBuilder()
                    .setLeaderId(leaderId)
                    .setName(name)
                    .setNotice(notice != null ? notice : "")
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] createGuild: guild-service unavailable");
            else log.error("[grpc-guild] createGuild error: {}", e.getMessage());
            return GuildInfoResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus joinGuild(String roleId, long guildId) {
        try {
            return stub.joinGuild(JoinGuildRequest.newBuilder().setRoleId(roleId).setGuildId(guildId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] joinGuild: guild-service unavailable");
            else log.error("[grpc-guild] joinGuild error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus leaveGuild(String roleId) {
        try {
            return stub.leaveGuild(LeaveGuildRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] leaveGuild: guild-service unavailable");
            else log.error("[grpc-guild] leaveGuild error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ListGuildsResponse listGuilds(int page, int size) {
        try {
            return stub.listGuilds(ListGuildsRequest.newBuilder().setPage(page).setSize(size).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-guild] listGuilds: guild-service unavailable");
            else log.error("[grpc-guild] listGuilds error: {}", e.getMessage());
            return ListGuildsResponse.newBuilder().setSuccess(false).build();
        }
    }
}
