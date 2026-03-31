package com.SouthMillion.webSocket_server.handler.rank;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.LeaderboardFeign;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgrank.Msgrank;
import org.SouthMillion.proto.Msgrole.Msgrole;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankHandler implements MessageHandler {

    private final LeaderboardFeign leaderboardFeign;

    private static final int RANK_POWER = 1;
    private static final int RANK_LEVEL = 2;
    private static final int RANK_ARENA = 3;
    private static final int RANK_GUILD = 4;
    private static final int RANK_PET   = 5;
    private static final int PAGE_SIZE  = 10;

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_RANK_REQ}; // PB_CSRankReq
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgrank.PB_CSRankReq req = Msgrank.PB_CSRankReq.parseFrom(payload);
                int type      = req.hasType()      ? req.getType()      : RANK_POWER;
                int listBegin = req.hasListBegin()  ? req.getListBegin() : 0;
                Long roleId = session.getRoleId();
                int page      = listBegin / PAGE_SIZE;

                log.debug("[Rank] type={}, listBegin={}, roleId={}", type, listBegin, roleId);

                // Gọi leaderboard-service với rankingType và roleId
                Map<String, Object> resp = leaderboardFeign.getLeaderboard(type, String.valueOf(roleId));

                Msgrank.PB_SCRankList.Builder response = Msgrank.PB_SCRankList.newBuilder();
                response.setType(type);
                response.setListBegin(listBegin);

                // Trích xuất danh sách từ response wrapper: { data: { rankings: [...], myRank: ..., myValue: ... } }
                List<Map<String, Object>> rankings = extractRankingList(resp);
                if (rankings != null) {
                    for (Map<String, Object> entry : rankings) {
                        Msgrank.PB_SCRankNode.Builder node = Msgrank.PB_SCRankNode.newBuilder();
                        node.setValue(getLong(entry, "value", 0L));
                        node.setRoleinfo(buildRoleInfo(entry));
                        response.addRanklist(node);
                    }
                }

                // Lấy rank/value của người chơi hiện tại từ cùng response
                try {
                    Map<String, Object> data = extractData(resp);
                    if (data != null) {
                        response.setMyRank(getInt(data, "myRank", 0));
                        response.setMyRankValue(getLong(data, "myValue", 0L));
                    }
                } catch (Exception e) {
                    log.debug("[Rank] Could not get player rank for roleId={}", roleId);
                }

                Emitters.emit(session, MsgIds.SC_RANK_LIST, response.build().toByteArray());

            } catch (Exception e) {
                log.error("[Rank] Error for roleId={}", session.getRoleId(), e);
                sendEmptyResponse(session, 0);
            }
        });
    }

    /** Trích xuất list rankings từ response wrapper của leaderboard-service */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRankingList(Map<String, Object> resp) {
        if (resp == null) return null;
        // Thử lấy resp.data.rankings
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object rankings = dataMap.get("rankings");
            if (rankings instanceof List<?> list) return (List<Map<String, Object>>) list;
        }
        // Fallback: nếu resp là list trực tiếp (legacy format)
        Object rankings = resp.get("rankings");
        if (rankings instanceof List<?> list) return (List<Map<String, Object>>) list;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> resp) {
        if (resp == null) return null;
        Object data = resp.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : null;
    }

    private Msgrole.PB_RoleInfo buildRoleInfo(Map<String, Object> entry) {
        Msgrole.PB_RoleInfo.Builder builder = Msgrole.PB_RoleInfo.newBuilder();
        try {
            Object roleIdVal = entry.get("roleId");
            if (roleIdVal instanceof Number n) builder.setRoleId(n.intValue());

            Object name = entry.get("roleName");
            if (name != null) builder.setName(ByteString.copyFromUtf8(name.toString()));

            Object level = entry.get("level");
            if (level instanceof Number n) builder.setLevel(n.intValue());

            Object cap = entry.get("power");
            if (cap instanceof Number n) builder.setCap(n.longValue());

            Object guildName = entry.get("guildName");
            if (guildName != null) builder.setGuildName(ByteString.copyFromUtf8(guildName.toString()));
        } catch (Exception e) {
            log.debug("[Rank] buildRoleInfo error: {}", e.getMessage());
        }
        return builder.build();
    }

    private void sendEmptyResponse(PlayerSession session, int type) {
        try {
            Msgrank.PB_SCRankList empty = Msgrank.PB_SCRankList.newBuilder()
                    .setType(type).setListBegin(0).build();
            Emitters.emit(session, 9601, empty.toByteArray());
        } catch (Exception e) {
            log.error("[Rank] sendEmptyResponse failed", e);
        }
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private long getLong(Map<String, Object> map, String key, long def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.longValue() : def;
    }
}
