package com.SouthMillion.webSocket_server.handler.chat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.ChatFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgchat.Msgchat;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat Handler - Real-time Messaging
 * 
 * Messages:
 * - C→S 1800 (CS_CHAT_REQ) - Client chat request
 * - S→C 1801 (SC_CHAT_SEND) - Send message response
 * - S→C 1802 (SC_CHAT_HISTORY) - Chat history response
 * - S→C 1803 (SC_CHAT_MUTE) - Mute/unmute response
 * 
 * Chat Channels:
 * - 1: World (public)
 * - 2: Guild
 * - 3: Team
 * - 4: Private (1-to-1)
 * - 5: System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatHandler.class);

    private final ChatFeign chatFeign;

    // Chat operation types from Frontend
    private static final int CHAT_OP_SEND = 1;        // Send message
    private static final int CHAT_OP_HISTORY = 2;     // Get history
    private static final int CHAT_OP_MUTE = 3;        // Mute player
    private static final int CHAT_OP_UNMUTE = 4;      // Unmute player

    @Override
    public int[] interests() {
        return new int[]{MessageIds.CS_CHAT_REQ}; // 1800
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        if (msgId != MessageIds.CS_CHAT_REQ) {
            log.warn("[chat] Unknown msgId: {}", msgId);
            return;
        }

        try {
            Msgchat.CS_CHAT_REQ req = Msgchat.CS_CHAT_REQ.parseFrom(payload);
            int op = req.getOp();
            Long roleId = ps.getRoleId();

            log.debug("[chat] op={}, roleId={}, channel={}", op, roleId, req.getChannel());

            switch (op) {
                case CHAT_OP_SEND:
                    handleSendMessage(ps, req);
                    break;
                case CHAT_OP_HISTORY:
                    handleGetHistory(ps, req);
                    break;
                case CHAT_OP_MUTE:
                    handleMutePlayer(ps, req);
                    break;
                case CHAT_OP_UNMUTE:
                    handleUnmutePlayer(ps, req);
                    break;
                default:
                    log.warn("[chat] Unknown operation: {}", op);
                    sendErrorResponse(ps, "Unknown operation");
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("[chat] Failed to parse CS_CHAT_REQ", e);
            sendErrorResponse(ps, "Invalid request format");
        } catch (Exception e) {
            log.error("[chat] Error handling chat request", e);
            sendErrorResponse(ps, "Internal error");
        }
        });
    }

    /**
     * Handle send message
     */
    private void handleSendMessage(PlayerSession ps, Msgchat.CS_CHAT_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("senderId", ps.getRoleId());
            request.put("channel", req.getChannel());
            request.put("content", req.getContent());
            if (req.hasReceiverId()) {
                request.put("receiverId", req.getReceiverId());
            }

            Map<String, Object> result = chatFeign.sendMessage(request);

            // Build response
            Msgchat.SC_CHAT_SEND.Builder builder = Msgchat.SC_CHAT_SEND.newBuilder()
                    .setCode(0)
                    .setMessage("Success");

            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                
                Msgchat.MessageInfo.Builder msgInfo = Msgchat.MessageInfo.newBuilder()
                        .setMessageId(((Number) data.get("messageId")).longValue())
                        .setSenderId(String.valueOf(data.get("senderId")))
                        .setSenderName(String.valueOf(data.get("senderName")))
                        .setChannel((Integer) data.get("channel"))
                        .setContent(String.valueOf(data.get("content")))
                        .setTimestamp(((Number) data.get("timestamp")).longValue());

                if (data.containsKey("receiverId")) {
                    msgInfo.setReceiverId(String.valueOf(data.get("receiverId")));
                }

                builder.setMessageInfo(msgInfo.build());
            }

            Emitters.emit(ps, MessageIds.SC_CHAT_SEND, builder.build().toByteArray());

            log.info("[chat] Message sent - roleId={}, channel={}", ps.getRoleId(), req.getChannel());

        } catch (Exception e) {
            log.error("[chat] Failed to send message", e);
            sendErrorResponse(ps, "Failed to send message");
        }
    }

    /**
     * Handle get chat history
     */
    private void handleGetHistory(PlayerSession ps, Msgchat.CS_CHAT_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roleId", ps.getRoleId());
            request.put("channel", req.getChannel());
            if (req.hasReceiverId()) {
                request.put("receiverId", req.getReceiverId());
            }

            Map<String, Object> result = chatFeign.getHistory(request);

            // Build response
            Msgchat.SC_CHAT_HISTORY.Builder builder = Msgchat.SC_CHAT_HISTORY.newBuilder()
                    .setCode(0);

            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) result.get("data");
                
                for (Map<String, Object> data : messages) {
                    Msgchat.MessageInfo.Builder msgInfo = Msgchat.MessageInfo.newBuilder()
                            .setMessageId(((Number) data.get("messageId")).longValue())
                            .setSenderId(String.valueOf(data.get("senderId")))
                            .setSenderName(String.valueOf(data.get("senderName")))
                            .setChannel((Integer) data.get("channel"))
                            .setContent(String.valueOf(data.get("content")))
                            .setTimestamp(((Number) data.get("timestamp")).longValue());

                    if (data.containsKey("receiverId")) {
                        msgInfo.setReceiverId(String.valueOf(data.get("receiverId")));
                    }

                    builder.addMessages(msgInfo);
                }
            }

            Emitters.emit(ps, MessageIds.SC_CHAT_HISTORY, builder.build().toByteArray());

            log.info("[chat] History sent - roleId={}, channel={}", ps.getRoleId(), req.getChannel());

        } catch (Exception e) {
            log.error("[chat] Failed to get history", e);
            sendErrorResponse(ps, "Failed to get history");
        }
    }

    /**
     * Handle mute player
     */
    private void handleMutePlayer(PlayerSession ps, Msgchat.CS_CHAT_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("adminId", ps.getRoleId());
            request.put("roleId", req.getTargetRoleId());
            request.put("duration", req.getMuteDuration());

            chatFeign.mutePlayer(request);

            // Send success response
            Msgchat.SC_CHAT_MUTE.Builder builder = Msgchat.SC_CHAT_MUTE.newBuilder()
                    .setCode(0);

            Emitters.emit(ps, MessageIds.SC_CHAT_MUTE, builder.build().toByteArray());

            log.info("[chat] Player muted - admin={}, target={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[chat] Failed to mute player", e);
            sendErrorResponse(ps, "Failed to mute player");
        }
    }

    /**
     * Handle unmute player
     */
    private void handleUnmutePlayer(PlayerSession ps, Msgchat.CS_CHAT_REQ req) {
        try {
            chatFeign.unmutePlayer(req.getTargetRoleId());

            // Send success response
            Msgchat.SC_CHAT_MUTE.Builder builder = Msgchat.SC_CHAT_MUTE.newBuilder()
                    .setCode(0);

            Emitters.emit(ps, MessageIds.SC_CHAT_MUTE, builder.build().toByteArray());

            log.info("[chat] Player unmuted - admin={}, target={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[chat] Failed to unmute player", e);
            sendErrorResponse(ps, "Failed to unmute player");
        }
    }

    /**
     * Send error response to client
     */
    private void sendErrorResponse(PlayerSession ps, String errorMsg) {
        try {
            Msgchat.SC_CHAT_SEND.Builder builder = Msgchat.SC_CHAT_SEND.newBuilder()
                    .setCode(1)
                    .setMessage(errorMsg);

            Emitters.emit(ps, MessageIds.SC_CHAT_SEND, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[chat] Failed to send error response", e);
        }
    }
}
