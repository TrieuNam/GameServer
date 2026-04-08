package com.SouthMillion.guild_service.grpc;

import com.SouthMillion.guild_service.dto.GuildDTO;
import com.SouthMillion.guild_service.entity.GuildMember;
import com.SouthMillion.guild_service.repository.GuildMemberRepository;
import com.SouthMillion.guild_service.service.GuildService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.guild.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * gRPC Server Implementation for Guild Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GuildServiceGrpcImpl extends GuildServiceGrpc.GuildServiceImplBase {

    private final GuildService guildService;
    private final GuildMemberRepository memberRepository;

    @Override
    public void getGuildInfo(GetGuildInfoRequest request, StreamObserver<GuildInfoResponse> responseObserver) {
        try {
            GuildDTO.Response<GuildDTO.InfoResponse> result = guildService.getGuildInfo(request.getGuildId());

            GuildInfoResponse response = buildGuildInfoResponse(result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-guild] GetGuildInfo success: guildId={}", request.getGuildId());

        } catch (Exception e) {
            log.error("[grpc-guild] GetGuildInfo failed: {}", e.getMessage(), e);
            responseObserver.onNext(GuildInfoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createGuild(CreateGuildRequest request, StreamObserver<GuildInfoResponse> responseObserver) {
        try {
            GuildDTO.CreateRequest dto = GuildDTO.CreateRequest.builder()
                    .name(request.getName())
                    .leaderId(request.getLeaderId())
                    .notice(request.getNotice().isEmpty() ? null : request.getNotice())
                    .build();

            GuildDTO.Response<GuildDTO.InfoResponse> result = guildService.createGuild(dto);

            GuildInfoResponse response = buildGuildInfoResponse(result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-guild] CreateGuild success: name={}, leaderId={}", request.getName(), request.getLeaderId());

        } catch (Exception e) {
            log.error("[grpc-guild] CreateGuild failed: {}", e.getMessage(), e);
            responseObserver.onNext(GuildInfoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void joinGuild(JoinGuildRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            // Apply to join guild (creates a pending application)
            GuildDTO.JoinRequest dto = GuildDTO.JoinRequest.builder()
                    .guildId(request.getGuildId())
                    .roleId(request.getRoleId())
                    .roleName(request.getRoleId()) // roleName not in proto; use roleId as placeholder
                    .roleLevel(1)
                    .power(0L)
                    .build();

            GuildDTO.Response<Void> result = guildService.applyToGuild(dto);

            boolean success = result.getCode() == 0;
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(success)
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .build());
            responseObserver.onCompleted();

            log.info("[grpc-guild] JoinGuild (apply) success: roleId={}, guildId={}", request.getRoleId(), request.getGuildId());

        } catch (Exception e) {
            log.error("[grpc-guild] JoinGuild failed: {}", e.getMessage(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void leaveGuild(LeaveGuildRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            // Look up the member's guildId from roleId
            Optional<GuildMember> memberOpt = memberRepository.findByRoleId(Long.parseLong(request.getRoleId()));
            if (memberOpt.isEmpty()) {
                responseObserver.onNext(ResponseStatus.newBuilder()
                        .setSuccess(false)
                        .setCode(-1)
                        .setMessage("Player is not in any guild")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Long guildId = memberOpt.get().getGuildId();
            GuildDTO.Response<Void> result = guildService.leaveGuild(guildId, request.getRoleId());

            boolean success = result.getCode() == 0;
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(success)
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .build());
            responseObserver.onCompleted();

            log.info("[grpc-guild] LeaveGuild success: roleId={}, guildId={}", request.getRoleId(), guildId);

        } catch (Exception e) {
            log.error("[grpc-guild] LeaveGuild failed: {}", e.getMessage(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getMemberGuild(GetMemberGuildRequest request, StreamObserver<GuildInfoResponse> responseObserver) {
        try {
            // Find the member's guild
            Optional<GuildMember> memberOpt = memberRepository.findByRoleId(Long.parseLong(request.getRoleId()));
            if (memberOpt.isEmpty()) {
                responseObserver.onNext(GuildInfoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Player is not in any guild")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Long guildId = memberOpt.get().getGuildId();
            GuildDTO.Response<GuildDTO.InfoResponse> result = guildService.getGuildInfo(guildId);

            GuildInfoResponse response = buildGuildInfoResponse(result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[grpc-guild] GetMemberGuild success: roleId={}, guildId={}", request.getRoleId(), guildId);

        } catch (Exception e) {
            log.error("[grpc-guild] GetMemberGuild failed: {}", e.getMessage(), e);
            responseObserver.onNext(GuildInfoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listGuilds(ListGuildsRequest request, StreamObserver<ListGuildsResponse> responseObserver) {
        try {
            int page = request.getPage();
            int size = request.getSize() > 0 ? request.getSize() : 20;

            GuildDTO.SearchRequest searchRequest = GuildDTO.SearchRequest.builder()
                    .page(page)
                    .size(size)
                    .build();

            GuildDTO.Response<GuildDTO.PageResponse<GuildDTO.ListItem>> result = guildService.searchGuilds(searchRequest);

            ListGuildsResponse.Builder builder = ListGuildsResponse.newBuilder();

            if (result.getCode() == 0 && result.getData() != null) {
                builder.setSuccess(true);
                builder.setTotal(result.getData().getTotal());

                List<GuildDTO.ListItem> items = result.getData().getContent();
                if (items != null) {
                    for (GuildDTO.ListItem item : items) {
                        builder.addGuilds(toProtoGuildData(item));
                    }
                }
            } else {
                builder.setSuccess(false);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

            log.info("[grpc-guild] ListGuilds success: page={}, size={}", page, size);

        } catch (Exception e) {
            log.error("[grpc-guild] ListGuilds failed: {}", e.getMessage(), e);
            responseObserver.onNext(ListGuildsResponse.newBuilder()
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ==================== Helper Methods ====================

    private GuildInfoResponse buildGuildInfoResponse(GuildDTO.Response<GuildDTO.InfoResponse> result) {
        boolean success = result.getCode() == 0;
        GuildInfoResponse.Builder builder = GuildInfoResponse.newBuilder()
                .setSuccess(success)
                .setMessage(result.getMessage() != null ? result.getMessage() : "");

        if (success && result.getData() != null) {
            builder.setGuild(toProtoGuildData(result.getData()));
        }

        return builder.build();
    }

    private GuildData toProtoGuildData(GuildDTO.InfoResponse info) {
        return GuildData.newBuilder()
                .setId(info.getId() != null ? info.getId() : 0L)
                .setName(info.getName() != null ? info.getName() : "")
                .setNotice(info.getNotice() != null ? info.getNotice() : "")
                .setLeaderId(info.getLeaderId() != null ? Long.parseLong(info.getLeaderId()) : 0L)
                .setLevel(info.getLevel() != null ? info.getLevel() : 1)
                .setExp(info.getExp() != null ? info.getExp() : 0L)
                .setMemberCount(info.getMemberCount() != null ? info.getMemberCount() : 0)
                .setMaxMembers(info.getMaxMembers() != null ? info.getMaxMembers() : 50)
                .setCreatedAtMs(info.getCreatedAt() != null
                        ? info.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : 0L)
                .build();
    }

    private GuildData toProtoGuildData(GuildDTO.ListItem item) {
        return GuildData.newBuilder()
                .setId(item.getId() != null ? item.getId() : 0L)
                .setName(item.getName() != null ? item.getName() : "")
                .setLevel(item.getLevel() != null ? item.getLevel() : 1)
                .setMemberCount(item.getMemberCount() != null ? item.getMemberCount() : 0)
                .setMaxMembers(item.getMaxMembers() != null ? item.getMaxMembers() : 50)
                .build();
    }
}
