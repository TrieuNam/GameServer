package com.SouthMillion.webSocket_server.handler.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.AnalyticsFeign;
import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsHandler implements MessageHandler {

    private static final int SESSION_ID_MAX_LENGTH = 50;

    private final AnalyticsFeign analyticsFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Operations (client → server)
    private static final int TRACK_EVENT = 1;
    private static final int GET_EVENTS  = 2;
    private static final int GET_KPI     = 3;

    // =========================================================
    // Public API — dùng cho WsGatewayHandler & các handler khác
    // =========================================================

    /**
     * Track một event đơn giản — fire-and-forget, không throw exception.
     *
     * @param ps        PlayerSession (lấy roleId / sessionId)
     * @param eventType Tên event, ví dụ: "LOGIN_SUCCESS", "DISPATCH_TIMEOUT"
     * @param category  Nhóm event, ví dụ: "AUTH", "SYSTEM", "GAMEPLAY"
     */
    public void track(PlayerSession ps, String eventType, String category) {
        track(ps, eventType, category, null);
    }

    /**
     * Track event kèm extra data — fire-and-forget, không throw exception.
     *
     * <p>An toàn khi gọi từ BẤT KỲ thread nào, kể cả reactor-http-nio (Netty I/O thread),
     * vì Feign call được offload sang {@code Schedulers.boundedElastic()} — tránh lỗi
     * {@code block()/blockFirst() are blocking, which is not supported in thread reactor-http-nio-N}.
     *
     * @param ps        PlayerSession
     * @param eventType Tên event
     * @param category  Nhóm event
     * @param extra     Dữ liệu bổ sung (nullable)
     */
    public void track(PlayerSession ps, String eventType, String category, Map<String, Object> extra) {
        try {
            Map<String, Object> req = new HashMap<>();
            String analyticsSessionId = resolveAnalyticsSessionId(ps);
            req.put("playerId",      ps != null ? ps.getRoleId()    : null);
            req.put("sessionId",     analyticsSessionId);
            req.put("eventType",     eventType);
            req.put("eventCategory", category != null ? category : "SYSTEM");
            req.put("timestamp",     System.currentTimeMillis());
            if (extra != null) req.putAll(extra);

            // Capture token TRƯỚC khi đổi thread — FeignTokenHolder là ThreadLocal
            final String token = ps != null ? ps.getSessionId() : null;

            // Offload blocking Feign call ra khỏi reactor / Netty I/O thread
            Mono.fromRunnable(() -> {
                try {
                    if (token != null && !token.isBlank()) {
                        FeignTokenHolder.set(token);
                    }
                    analyticsFeign.trackEvent(req);
                } catch (Exception e) {
                    log.debug("[analytics] track failed: eventType={}, err={}", eventType, e.getMessage());
                } finally {
                    FeignTokenHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        } catch (Exception e) {
            log.debug("[analytics] track build failed: eventType={}, err={}", eventType, e.getMessage());
        }
    }

    private String resolveAnalyticsSessionId(PlayerSession ps) {
        if (ps == null) {
            return null;
        }

        String sessionId = normalizeSessionId(ps.getAnalyticsSessionId());
        if (sessionId != null) {
            return sessionId;
        }

        sessionId = normalizeSessionId(ps.getSessionId());
        if (sessionId != null && (ps.getAnalyticsSessionId() == null || ps.getAnalyticsSessionId().isBlank())) {
            ps.setAnalyticsSessionId(sessionId);
        }
        return sessionId;
    }

    private String normalizeSessionId(String rawSessionId) {
        if (rawSessionId == null) {
            return null;
        }

        String sessionId = rawSessionId.trim();
        if (sessionId.isEmpty()) {
            return null;
        }
        if (sessionId.length() <= SESSION_ID_MAX_LENGTH) {
            return sessionId;
        }

        String sidFromJwt = extractSidFromJwt(sessionId);
        if (sidFromJwt != null && sidFromJwt.length() <= SESSION_ID_MAX_LENGTH) {
            return sidFromJwt;
        }

        log.debug("[analytics] drop oversized non-normalizable sessionId len={}", sessionId.length());
        return null;
    }

    private String extractSidFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || parts[1].isBlank()) {
                return null;
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode sidNode = claims.get("sid");
            if (sidNode == null || sidNode.isNull()) {
                return null;
            }

            String sid = sidNode.asText(null);
            return sid == null || sid.isBlank() ? null : sid.trim();
        } catch (Exception e) {
            log.debug("[analytics] failed to extract sid from session token: {}", e.getMessage());
            return null;
        }
    }

    // =========================================================
    // MessageHandler — xử lý request từ client (msgId 9200)
    // =========================================================

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_ANALYTICS_REQ};
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Map<String, Object> data = objectMapper.readValue(
                        payload, new TypeReference<>() {});
                int operation = ((Number) data.getOrDefault("op", 0)).intValue();

                switch (operation) {
                    case TRACK_EVENT -> handleTrackEvent(ps, data);
                    case GET_EVENTS  -> handleGetEvents(ps, data);
                    case GET_KPI     -> handleGetKpi(ps, data);
                    default -> log.warn("Unknown analytics operation: {}", operation);
                }

            } catch (Exception e) {
                log.error("Failed to handle analytics message", e);
            }
        });
    }

    // =========================================================
    // Private handlers
    // =========================================================

    private void handleTrackEvent(PlayerSession ps, Map<String, Object> data) {
        try {
            String eventType     = (String) data.get("eventType");
            String eventCategory = (String) data.getOrDefault("eventCategory", "GAMEPLAY");
            track(ps, eventType, eventCategory, null);
            log.info("[analytics] tracked event={} player={}", eventType, ps.getRoleId());

            Emitters.emit(ps, MsgIds.SC_ANALYTICS_TRACK_ACK,
                    objectMapper.writeValueAsBytes(Map.of("op", TRACK_EVENT, "result", 0, "msg", "ok")));
        } catch (Exception e) {
            log.error("Failed to track event", e);
            sendErrorAck(ps, TRACK_EVENT, e.getMessage());
        }
    }

    private void handleGetEvents(PlayerSession ps, Map<String, Object> data) {
        try {
            Long playerId = ps.getRoleId();
            String startTime = data.containsKey("startTime")
                    ? String.valueOf(data.get("startTime"))
                    : String.valueOf(System.currentTimeMillis() - 86400000L);
            String endTime = data.containsKey("endTime")
                    ? String.valueOf(data.get("endTime"))
                    : String.valueOf(System.currentTimeMillis());

            List<Map<String, Object>> events = analyticsFeign.getPlayerEvents(playerId, startTime, endTime);
            log.info("[analytics] retrieved {} events for player={}", events != null ? events.size() : 0, playerId);

            Map<String, Object> resp = Map.of(
                    "op", GET_EVENTS, "result", 0,
                    "events",    events    != null ? events    : List.of(),
                    "startTime", startTime,
                    "endTime",   endTime
            );
            Emitters.emit(ps, MsgIds.SC_ANALYTICS_EVENTS, objectMapper.writeValueAsBytes(resp));
        } catch (Exception e) {
            log.error("Failed to get events", e);
            sendErrorAck(ps, GET_EVENTS, e.getMessage());
        }
    }

    private void handleGetKpi(PlayerSession ps, Map<String, Object> data) {
        try {
            Long playerId = ps.getRoleId();
            String date = data.containsKey("date")
                    ? String.valueOf(data.get("date"))
                    : String.valueOf(System.currentTimeMillis());

            Map<String, Object> kpi = analyticsFeign.getPlayerKpi(playerId, date);
            log.info("[analytics] retrieved KPI for player={}", playerId);

            Map<String, Object> resp = Map.of(
                    "op", GET_KPI, "result", 0,
                    "date", date,
                    "kpi",  kpi != null ? kpi : Map.of()
            );
            Emitters.emit(ps, MsgIds.SC_ANALYTICS_KPI, objectMapper.writeValueAsBytes(resp));
        } catch (Exception e) {
            log.error("Failed to get KPI", e);
            sendErrorAck(ps, GET_KPI, e.getMessage());
        }
    }

    private void sendErrorAck(PlayerSession ps, int op, String errorMsg) {
        try {
            int scMsgId = switch (op) {
                case TRACK_EVENT -> MsgIds.SC_ANALYTICS_TRACK_ACK;
                case GET_EVENTS  -> MsgIds.SC_ANALYTICS_EVENTS;
                case GET_KPI     -> MsgIds.SC_ANALYTICS_KPI;
                default          -> MsgIds.SC_ANALYTICS_TRACK_ACK;
            };
            Emitters.emit(ps, scMsgId, objectMapper.writeValueAsBytes(Map.of(
                    "op", op, "result", -1,
                    "msg", errorMsg != null ? errorMsg : "internal error"
            )));
        } catch (Exception ex) {
            log.error("Failed to send analytics error ack", ex);
        }
    }
}
