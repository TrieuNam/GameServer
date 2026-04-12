package com.SouthMillion.webSocket_server.handler.guild;

import com.google.protobuf.ByteString;
import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.LazyLoadHandler;
import com.SouthMillion.webSocket_server.handler.common.ProtoRequestWrapper;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.GuildFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgguild.Msgguild;
import org.SouthMillion.proto.Msgrole.Msgrole;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Guild System Handler - Using ProtoRequestWrapper for proto compatibility
 *
 * Request Types from client:
 * - 1: Guild Info
 * - 2: Create Guild
 * - 3: Join Guild
 * - 4: Leave Guild
 * - 5: Guild Donate
 * - 6: Guild Tech
 * - 7: Guild Members
 * - 8: Search Guilds
 * - 9: Apply to Guild
 * - 10: Guild Approve
 *
 * This handler implements LazyLoadHandler for on-demand loading when the player opens the guild UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuildHandler implements MessageHandler, LazyLoadHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuildHandler.class);

    private final GuildFeign guildFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    @Override
    public int[] interests() {
        return new int[]{MessageIds.CS_GUILD_REQ}; // 9640 (fixed from 2000)
    }

    @Override
    public String getModuleName() {
        return "guild";
    }

    @Override
    public Mono<Void> loadOnDemand(PlayerSession ps) {
        log.info("[Guild] Lazy loading for roleId={}", ps.getRoleId());
        return pushAll(ps);
    }

    /** Gọi sau login: đẩy thông tin bang hội (9642) về client (guildId=0 nếu chưa có bang). */
    public Mono<Void> pushAll(PlayerSession session) {
        if (session.getRoleId() == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGuildInfo(session, null));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        try {
            // Parse proto request
            Msgguild.PB_CSGuildReq req = Msgguild.PB_CSGuildReq.parseFrom(payload);
            
            // Wrap request for convenient access
            ProtoRequestWrapper wrapper = new ProtoRequestWrapper(req);
            int operation = wrapper.getOp();
            
            log.debug("[Guild] Handler op={}, roleId={}", operation, session.getRoleId());
            
            // Route to specific handler based on operation
            switch (operation) {
                case 1 -> handleGuildInfo(session, wrapper);
                case 2 -> handleCreateGuild(session, wrapper);
                case 3 -> handleJoinGuild(session, wrapper);
                case 4 -> handleLeaveGuild(session, wrapper);
                case 5 -> handleGuildDonate(session, wrapper);
                case 6 -> handleGuildTech(session, wrapper);
                case 7 -> handleGuildMembers(session, wrapper);
                case 8 -> handleSearchGuilds(session, wrapper);
                case 9 -> handleApplyToGuild(session, wrapper);
                case 10 -> handleGuildApprove(session, wrapper);
                default -> log.warn("[Guild] Unknown operation: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("[Guild] Error handling guild request for roleId={}", session.getRoleId(), e);
        }
        });
    }
    
    private void handleGuildInfo(PlayerSession session, ProtoRequestWrapper req) {
        log.info("[Guild] Get guild info - roleId={}", session.getRoleId());
        
        try {
            Map<String, Object> guildData = guildFeign.getPlayerGuild(String.valueOf(session.getRoleId()));
            
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            if (guildData != null && !guildData.isEmpty()) {
                response.setGuildId(((Number) guildData.getOrDefault("guildId", 0)).intValue());
                Object name = guildData.get("name");
                if (name instanceof String) response.setGuildName(com.google.protobuf.ByteString.copyFromUtf8((String) name));
                Object memberNum = guildData.get("memberCount");
                if (memberNum instanceof Number) response.setGuildMemberNum(((Number) memberNum).intValue());
                Object pos = guildData.get("myPosition");
                if (pos instanceof Number) response.setMPosition(((Number) pos).intValue());
            } else {
                response.setGuildId(0); // No guild
            }
            sendGuildInfoResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Guild] Failed to get guild info", e);
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            response.setGuildId(0);
            sendGuildInfoResponse(session, response.build());
        }
    }
    
    private void handleCreateGuild(PlayerSession session, ProtoRequestWrapper req) {
        String guildName = req.getStrParam(); // Guild name from str_param
        log.info("[Guild] Create guild '{}' - roleId={}", guildName, session.getRoleId());
        
        try {
            Map<String, Object> request = Map.of(
                "roleId", String.valueOf(session.getRoleId()),
                "name", guildName
            );
            Map<String, Object> result = guildFeign.createGuild(request);
            
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            boolean success = false;
            if (result != null) {
                int createdGuildId = ((Number) result.getOrDefault("guildId", 0)).intValue();
                response.setGuildId(createdGuildId);
                response.setGuildName(com.google.protobuf.ByteString.copyFromUtf8((String) result.getOrDefault("name", guildName)));
                success = createdGuildId > 0;
            }
            if (success) {
                publishTaskProgress(session.getRoleId(), taskActionConditionMapping.guildCreateTaskKey(), "websocket-guild-create");
            }
            sendGuildInfoResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Guild] Failed to create guild", e);
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            response.setGuildId(0);
            sendGuildInfoResponse(session, response.build());
        }
    }
    
    private void handleJoinGuild(PlayerSession session, ProtoRequestWrapper req) {
        int guildId = req.getGuildId(); // param[0]
        log.info("[Guild] Join guild {} - roleId={}", guildId, session.getRoleId());

        try {
            // GuildController mới: POST /api/guild/apply với body {roleId, guildId}
            Map<String, Object> body = Map.of(
                "roleId", String.valueOf(session.getRoleId()),
                "guildId", (long) guildId
            );
            Map<String, Object> result = guildFeign.joinGuild(body);
            boolean success = result != null && Boolean.TRUE.equals(result.get("success"));
            log.info("[Guild] Join guild result: success={}", success);
            if (success) {
                publishTaskProgress(session.getRoleId(), taskActionConditionMapping.guildJoinTaskKey(), "websocket-guild-join");
            }
            handleGuildInfo(session, req);
        } catch (Exception e) {
            log.error("[Guild] Failed to join guild {}", guildId, e);
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            response.setGuildId(0);
            sendGuildInfoResponse(session, response.build());
        }
    }

    private void handleLeaveGuild(PlayerSession session, ProtoRequestWrapper req) {
        log.info("[Guild] Leave guild - roleId={}", session.getRoleId());

        try {
            Long guildId = getPlayerGuildId(session);
            if (guildId == null) {
                log.warn("[Guild] Player is not in a guild: roleId={}", session.getRoleId());
            } else {
                guildFeign.leaveGuild(guildId, String.valueOf(session.getRoleId()));
                log.info("[Guild] Left guild {}: roleId={}", guildId, session.getRoleId());
            }
        } catch (Exception e) {
            log.error("[Guild] Failed to leave guild", e);
        }
        Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
        response.setGuildId(0);
        sendGuildInfoResponse(session, response.build());
    }

    private void handleGuildDonate(PlayerSession session, ProtoRequestWrapper req) {
        int donationType = req.getDonationType(); // param[0]
        int amount = req.getAmount();             // param[1]
        log.info("[Guild] Guild donate type={}, amount={} - roleId={}", donationType, amount, session.getRoleId());

        try {
            Long guildId = getPlayerGuildId(session);
            if (guildId != null) {
                // GuildController mới: POST /api/guild/donate (guildId trong body)
                Map<String, Object> body = Map.of(
                    "roleId", String.valueOf(session.getRoleId()),
                    "guildId", guildId,
                    "donationType", donationType,
                    "amount", amount
                );
                Map<String, Object> result = guildFeign.donate(body);
                log.info("[Guild] Donate result: {}", result);
            }
            handleGuildInfo(session, req);
        } catch (Exception e) {
            log.error("[Guild] Failed to donate", e);
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            sendGuildInfoResponse(session, response.build());
        }
    }

    private void handleGuildTech(PlayerSession session, ProtoRequestWrapper req) {
        int techBranch = req.getTechBranch(); // param[0]
        int techId = req.getTechId();         // param[2]
        log.info("[Guild] Guild tech upgrade branch={}, techId={} - roleId={}", techBranch, techId, session.getRoleId());

        try {
            Long guildId = getPlayerGuildId(session);
            if (guildId != null) {
                // GuildController mới: POST /api/guild/tech/upgrade (guildId trong body)
                Map<String, Object> body = Map.of(
                    "roleId", String.valueOf(session.getRoleId()),
                    "guildId", guildId,
                    "techType", techBranch,
                    "techId", techId
                );
                Map<String, Object> result = guildFeign.upgradeTech(body);
                log.info("[Guild] Tech upgrade result: {}", result);
            }
            handleGuildInfo(session, req);
        } catch (Exception e) {
            log.error("[Guild] Failed to upgrade tech", e);
            Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
            sendGuildInfoResponse(session, response.build());
        }
    }

    private void handleGuildMembers(PlayerSession session, ProtoRequestWrapper req) {
        log.info("[Guild] Get guild members - roleId={}", session.getRoleId());

        try {
            Long guildId = getPlayerGuildId(session);
            Msgguild.PB_SCGuildMemberList.Builder response = Msgguild.PB_SCGuildMemberList.newBuilder();
            if (guildId != null) {
                List<Map<String, Object>> members = guildFeign.getMembers(guildId);
                if (members != null) {
                    for (Map<String, Object> member : members) {
                        Msgrole.PB_RoleInfo.Builder roleInfo = Msgrole.PB_RoleInfo.newBuilder();
                        Object rid = member.get("roleId");
                        if (rid instanceof Number) roleInfo.setRoleId(((Number) rid).intValue());
                        Object name = member.get("roleName");
                        if (name instanceof String) roleInfo.setName(com.google.protobuf.ByteString.copyFromUtf8((String) name));
                        Object lv = member.get("level");
                        if (lv instanceof Number) roleInfo.setLevel(((Number) lv).intValue());
                        Object cap = member.get("fightPower");
                        if (cap instanceof Number) roleInfo.setCap(((Number) cap).longValue());

                        Msgguild.PB_SCGuildMemberNode.Builder memberNode = Msgguild.PB_SCGuildMemberNode.newBuilder();
                        memberNode.setRoleInfo(roleInfo.build());
                        Object pos = member.get("position");
                        if (pos instanceof Number) memberNode.setPosition(((Number) pos).intValue());
                        response.addMemberList(memberNode.build());
                    }
                }
            }
            sendGuildMembersResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Guild] Failed to get members", e);
            sendGuildMembersResponse(session, Msgguild.PB_SCGuildMemberList.newBuilder().build());
        }
    }
    
    private void handleSearchGuilds(PlayerSession session, ProtoRequestWrapper req) {
        String searchKeyword = req.getStrParam();
        log.info("[Guild] Search guilds keyword='{}' - roleId={}", searchKeyword, session.getRoleId());
        
        try {
            // GuildController mới: POST /api/guild/search với body {keyword, page, size}
            Map<String, Object> searchBody = Map.of(
                "keyword", searchKeyword,
                "page", 0,
                "size", 20
            );
            List<Map<String, Object>> guilds = guildFeign.searchGuilds(searchBody);
            
            Msgguild.PB_SCGuildSearchList.Builder response = Msgguild.PB_SCGuildSearchList.newBuilder();
            if (guilds != null) {
                for (Map<String, Object> guild : guilds) {
                    Msgguild.PB_SCGuildNode.Builder guildNode = Msgguild.PB_SCGuildNode.newBuilder();
                    Object gid = guild.get("guildId");
                    if (gid instanceof Number) guildNode.setGuildId(((Number) gid).intValue());
                    Object gname = guild.get("name");
                    if (gname instanceof String) guildNode.setGuildName(com.google.protobuf.ByteString.copyFromUtf8((String) gname));
                    Object memberNum = guild.get("memberCount");
                    if (memberNum instanceof Number) guildNode.setGuildMemberNum(((Number) memberNum).intValue());
                    response.addGuildList(guildNode.build());
                }
            }
            sendGuildSearchResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Guild] Failed to search guilds", e);
            sendGuildSearchResponse(session, Msgguild.PB_SCGuildSearchList.newBuilder().build());
        }
    }
    
    private void handleApplyToGuild(PlayerSession session, ProtoRequestWrapper req) {
        int guildId = req.getGuildId(); // param[0]
        log.info("[Guild] Apply to guild {} - roleId={}", guildId, session.getRoleId());

        try {
            // GuildController mới: POST /api/guild/apply với body {roleId, guildId}
            Map<String, Object> body = Map.of(
                "roleId", String.valueOf(session.getRoleId()),
                "guildId", (long) guildId
            );
            Map<String, Object> result = guildFeign.joinGuild(body);
            log.info("[Guild] Apply result: {}", result);
        } catch (Exception e) {
            log.error("[Guild] Failed to apply to guild {}", guildId, e);
        }
        Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
        response.setGuildId(0);
        sendGuildInfoResponse(session, response.build());
    }

    private void handleGuildApprove(PlayerSession session, ProtoRequestWrapper req) {
        int targetRoleId = req.getParam(0); // param[0] = targetRoleId to approve/reject
        boolean approved = req.getApproved(); // param[2]
        log.info("[Guild] Approve application targetRoleId={}, approved={} - roleId={}",
                 targetRoleId, approved, session.getRoleId());

        try {
            Long guildId = getPlayerGuildId(session);
            if (guildId != null) {
                // GuildController mới: POST /api/guild/application/process
                // Body: {applicationId, processorId, approve}
                // Tạm dùng targetRoleId như applicationId (cần điều chỉnh nếu backend thay đổi)
                Map<String, Object> body = Map.of(
                    "applicationId", (long) targetRoleId,
                    "processorId", String.valueOf(session.getRoleId()),
                    "approve", approved
                );
                Map<String, Object> result = guildFeign.approveApplication(body);
                log.info("[Guild] Approve result: {}", result);
                handleGuildInfo(session, req);
                return;
            }
        } catch (Exception e) {
            log.error("[Guild] Failed to approve application", e);
        }
        Msgguild.PB_SCGuildInfo.Builder response = Msgguild.PB_SCGuildInfo.newBuilder();
        sendGuildInfoResponse(session, response.build());
    }

    /** Helper: fetch current guild id for the player, returns null if not in a guild */
    private Long getPlayerGuildId(PlayerSession session) {
        try {
            Map<String, Object> guildData = guildFeign.getPlayerGuild(String.valueOf(session.getRoleId()));
            if (guildData != null && guildData.containsKey("guildId")) {
                Object gid = guildData.get("guildId");
                if (gid instanceof Number) return ((Number) gid).longValue();
            }
        } catch (Exception e) {
            log.warn("[Guild] Failed to fetch player guild id for roleId={}: {}", session.getRoleId(), e.getMessage());
        }
        return null;
    }
    
    private void sendGuildInfoResponse(PlayerSession session, Msgguild.PB_SCGuildInfo response) {
        try {
            Emitters.emit(session, MessageIds.SC_GUILD_INFO, response.toByteArray()); // 9642
        } catch (Exception e) {
            log.error("[Guild] Failed to send response to roleId={}", session.getRoleId(), e);
        }
    }
    
    private void sendGuildMembersResponse(PlayerSession session, Msgguild.PB_SCGuildMemberList response) {
        try {
            Emitters.emit(session, MessageIds.SC_GUILD_MEMBER_LIST, response.toByteArray()); // 9644
        } catch (Exception e) {
            log.error("[Guild] Failed to send members response", e);
        }
    }

    private void sendGuildSearchResponse(PlayerSession session, Msgguild.PB_SCGuildSearchList response) {
        try {
            Emitters.emit(session, MessageIds.SC_GUILD_SEARCH_LIST, response.toByteArray()); // 9641
        } catch (Exception e) {
            log.error("[Guild] Failed to send search response", e);
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}
