package com.SouthMillion.webSocket_server.handler;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.NotificationFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket handler for notification system
 * Message ID: 9100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler implements MessageHandler {
    
    private final NotificationFeign notificationFeign;
    private final ObjectMapper objectMapper;
    
    private static final int OP_GET_NOTIFICATIONS = 1;
    private static final int OP_GET_UNREAD = 2;
    private static final int OP_MARK_READ = 3;
    
    @Override
    public int[] interests() {
        return new int[]{9100};
    }
    
    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        log.debug("Handling notification message, msgId={}, roleId={}", msgId, ps.getRoleId());
        
        try {
            byte[] safePayload = payload == null ? new byte[0] : payload;
            int operation = safePayload.length > 0 ? Byte.toUnsignedInt(safePayload[0]) : OP_GET_UNREAD;
            
            switch (operation) {
                case OP_GET_NOTIFICATIONS:
                    handleGetNotifications(ps);
                    break;
                case OP_GET_UNREAD:
                    handleGetUnread(ps);
                    break;
                case OP_MARK_READ:
                    handleMarkRead(ps, safePayload);
                    break;
                default:
                    log.warn("Unknown operation: {}", operation);
            }
        } catch (Exception e) {
            log.error("Error handling notification message", e);
        }
        });
    }
    
    private void handleGetNotifications(PlayerSession ps) {
        Long roleId = ps.getRoleId();
        List<Map<String, Object>> notifications = notificationFeign.getPlayerNotifications(roleId);
        List<Map<String, Object>> safeNotifications = normalizeNotifications(notifications);
        log.debug("Retrieved {} notifications for player {}", safeNotifications.size(), roleId);
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(safeNotifications);
            Emitters.emit(ps, 9100, jsonBytes);
        } catch (Exception ex) {
            log.error("[notification] Failed to send getAll response", ex);
        }
    }
    
    private void handleGetUnread(PlayerSession ps) {
        Long roleId = ps.getRoleId();
        List<Map<String, Object>> unread = notificationFeign.getUnreadNotifications(roleId);
        List<Map<String, Object>> safeUnread = normalizeNotifications(unread);
        log.debug("Retrieved {} unread notifications for player {}", safeUnread.size(), roleId);
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(safeUnread);
            Emitters.emit(ps, 9100, jsonBytes);
        } catch (Exception ex) {
            log.error("[notification] Failed to send getUnread response", ex);
        }
    }
    
    private void handleMarkRead(PlayerSession ps, byte[] payload) {
        // payload[0] = operation byte; payload[1..8] = notificationId as 8-byte big-endian long
        if (payload.length < 9) {
            log.warn("[notification] MarkRead: payload too short ({}), need at least 9 bytes", payload.length);
            return;
        }
        long notificationId = parseNotificationId(payload);
        Map<String, Object> markResp = notificationFeign.markAsRead(notificationId);
        log.debug("Marked notification {} as read for roleId={}", notificationId, ps.getRoleId());
        try {
            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("success", true);
            ack.put("notificationId", notificationId);
            if (markResp != null && !markResp.isEmpty()) {
                ack.put("result", markResp);
            }
            byte[] jsonBytes = objectMapper.writeValueAsBytes(ack);
            Emitters.emit(ps, 9100, jsonBytes);
        } catch (Exception ex) {
            log.error("[notification] Failed to send markRead response", ex);
        }
    }

    private long parseNotificationId(byte[] payload) {
        long bigEndian = ByteBuffer.wrap(payload, 1, 8).order(ByteOrder.BIG_ENDIAN).getLong();
        if (bigEndian > 0) {
            return bigEndian;
        }
        long littleEndian = ByteBuffer.wrap(payload, 1, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return littleEndian > 0 ? littleEndian : bigEndian;
    }

    private List<Map<String, Object>> normalizeNotifications(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(source.size());
        for (Map<String, Object> raw : source) {
            if (raw == null) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("id", raw.get("id"));
            normalized.put("playerId", raw.get("playerId"));
            normalized.put("type", raw.get("type"));
            normalized.put("title", raw.get("title"));
            normalized.put("message", raw.get("message"));
            normalized.put("data", raw.get("data"));
            normalized.put("status", raw.get("status"));
            normalized.put("createdAt", raw.get("createdAt"));
            normalized.put("sentAt", raw.get("sentAt"));
            normalized.put("readAt", raw.get("readAt"));
            normalized.put("errorMessage", raw.get("errorMessage"));
            out.add(normalized);
        }
        return out;
    }
}
