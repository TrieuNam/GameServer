package com.SouthMillion.webSocket_server.handler.mail;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.MailFeign;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.msgmail.Msgmail;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Handles mail operations from the client.
 *
 * Proto: PB_CSMailReq (9551) — type + p_1 + p_2
 * Responses:
 *   9501 PB_SCMailDeleteAck  — delete / mark-read ack
 *   9504 PB_SCMailListAck    — mail list
 *   9505 PB_SCMailDetail     — mail detail
 *   9506 PB_SCFetchMailAck   — fetch attachment ack
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailHandler implements MessageHandler {

    private final MailFeign mailFeign;

    private static final int OP_GET_LIST        = 0;
    private static final int OP_GET_DETAIL      = 1;
    private static final int OP_DELETE          = 2;
    private static final int OP_FETCH           = 3;
    private static final int OP_MARK_READ       = 4;
    private static final int OP_DELETE_ALL_READ = 5;
    private static final int OP_FETCH_ALL       = 6;

    @Override
    public int[] interests() {
        return new int[]{9551}; // PB_CSMailReq
    }

    /** Gọi sau login: đẩy danh sách thư (9504) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetList(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgmail.PB_CSMailReq req = Msgmail.PB_CSMailReq.parseFrom(payload);
                int type  = req.hasType() ? req.getType() : 0;
                int p1    = req.hasP1()   ? req.getP1()   : 0;
                int p2    = req.hasP2()   ? req.getP2()   : 0;
                Long roleId = session.getRoleId();

                log.debug("[Mail] op={}, p1={}, p2={}, roleId={}", type, p1, p2, roleId);

                switch (type) {
                    case OP_GET_LIST        -> handleGetList(session, roleId);
                    case OP_GET_DETAIL      -> handleGetDetail(session, (long) p1);
                    case OP_DELETE          -> handleDelete(session, p1, p2);
                    case OP_FETCH           -> handleFetch(session, roleId, (long) p1, p2);
                    case OP_MARK_READ       -> handleMarkRead(session, p1, p2);
                    case OP_DELETE_ALL_READ -> handleDeleteAllRead(session, roleId);
                    case OP_FETCH_ALL       -> handleFetchAll(session, roleId);
                    default -> {
                        log.warn("[Mail] Unknown op={}", type);
                        sendDeleteAck(session, 0, 0, -1);
                    }
                }
            } catch (Exception e) {
                log.error("[Mail] Error for roleId={}", session.getRoleId(), e);
                sendDeleteAck(session, 0, 0, -1);
            }
        });
    }

    // op=0: Get mail list → 9504 PB_SCMailListAck
    private void handleGetList(PlayerSession session, Long roleId) {
        try {
            Map<String, Object> result = mailFeign.getMailList(String.valueOf(roleId));
            Msgmail.PB_SCMailListAck.Builder builder = Msgmail.PB_SCMailListAck.newBuilder();
            if (result != null) {
                Object mailsObj = result.get("mails");
                if (mailsObj instanceof List<?> mails) {
                    for (Object item : mails) {
                        if (item instanceof Map<?, ?> mail) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) mail;
                            builder.addMailBriefData(buildBriefData(m));
                        }
                    }
                }
            }
            Emitters.emit(session, 9504, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Mail] handleGetList error", e);
            Emitters.emit(session, 9504, Msgmail.PB_SCMailListAck.newBuilder().build().toByteArray());
        }
    }

    // op=1: Get mail detail → 9505 PB_SCMailDetail
    private void handleGetDetail(PlayerSession session, long mailId) {
        try {
            Map<String, Object> result = mailFeign.getMailDetail(mailId);
            Msgmail.PB_SCMailDetail.Builder builder = Msgmail.PB_SCMailDetail.newBuilder();
            if (result != null) {
                builder.setMailType(getInt(result, "type", 1));
                builder.setMailIndex((int) mailId);
                Object title = result.get("title");
                if (title != null) builder.setSubject(ByteString.copyFromUtf8(title.toString()));
                Object content = result.get("content");
                if (content != null) builder.setContenttxt(ByteString.copyFromUtf8(content.toString()));
            }
            Emitters.emit(session, 9505, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Mail] handleGetDetail error for mailId={}", mailId, e);
            Emitters.emit(session, 9505, Msgmail.PB_SCMailDetail.newBuilder().build().toByteArray());
        }
    }

    // op=2: Delete mail → 9501 PB_SCMailDeleteAck
    private void handleDelete(PlayerSession session, int mailId, int mailType) {
        try {
            mailFeign.deleteMail((long) mailId);
            sendDeleteAck(session, mailType, mailId, 0);
        } catch (Exception e) {
            log.error("[Mail] handleDelete error for mailId={}", mailId, e);
            sendDeleteAck(session, mailType, mailId, -1);
        }
    }

    // op=3: Fetch mail attachment → 9506 PB_SCFetchMailAck
    private void handleFetch(PlayerSession session, Long roleId, long mailId, int mailType) {
        try {
            mailFeign.fetchAttachment(mailId, String.valueOf(roleId));
            sendFetchAck(session, mailType, (int) mailId, 0);
        } catch (Exception e) {
            log.error("[Mail] handleFetch error for mailId={}", mailId, e);
            sendFetchAck(session, mailType, (int) mailId, -1);
        }
    }

    // op=4: Mark mail as read → 9501 PB_SCMailDeleteAck (HTTP method now PUT)
    private void handleMarkRead(PlayerSession session, int mailId, int mailType) {
        try {
            mailFeign.markAsRead((long) mailId);
            sendDeleteAck(session, mailType, mailId, 0);
        } catch (Exception e) {
            log.error("[Mail] handleMarkRead error for mailId={}", mailId, e);
            sendDeleteAck(session, mailType, mailId, -1);
        }
    }

    // op=5: Delete all read mails → 9501 PB_SCMailDeleteAck
    private void handleDeleteAllRead(PlayerSession session, Long roleId) {
        try {
            mailFeign.deleteAllReadMails(String.valueOf(roleId));
            sendDeleteAck(session, 0, 0, 0);
        } catch (Exception e) {
            log.error("[Mail] handleDeleteAllRead error for roleId={}", roleId, e);
            sendDeleteAck(session, 0, 0, -1);
        }
    }

    // op=6: Fetch all attachments → 9506 PB_SCFetchMailAck
    private void handleFetchAll(PlayerSession session, Long roleId) {
        try {
            mailFeign.fetchAllAttachments(String.valueOf(roleId));
            sendFetchAck(session, 0, 0, 0);
        } catch (Exception e) {
            log.error("[Mail] handleFetchAll error for roleId={}", roleId, e);
            sendFetchAck(session, 0, 0, -1);
        }
    }

    private Msgmail.PB_MailBriefData buildBriefData(Map<String, Object> mail) {
        Msgmail.PB_MailBriefData.Builder b = Msgmail.PB_MailBriefData.newBuilder();
        b.setMailType(getInt(mail, "type", 1));
        Object mailId = mail.get("id");
        if (mailId instanceof Number n) b.setMailIndex(n.intValue());
        Object createdAt = mail.get("createdAt");
        if (createdAt instanceof Number n) b.setRecvTime(n.intValue());
        b.setIsRead(getBool(mail, "isRead") ? 1 : 0);
        b.setIsFetch(getBool(mail, "isClaimedAttachment") ? 1 : 0);
        Object title = mail.get("title");
        if (title != null) b.setSubject(ByteString.copyFromUtf8(title.toString()));
        return b.build();
    }

    private void sendDeleteAck(PlayerSession session, int mailType, int mailIndex, int ret) {
        try {
            Msgmail.PB_MailAckInfo ack = Msgmail.PB_MailAckInfo.newBuilder()
                    .setMailType(mailType).setMailIndex(mailIndex).setRet(ret).build();
            Msgmail.PB_SCMailDeleteAck resp = Msgmail.PB_SCMailDeleteAck.newBuilder()
                    .addAskInfo(ack).build();
            Emitters.emit(session, 9501, resp.toByteArray());
        } catch (Exception e) {
            log.error("[Mail] sendDeleteAck failed", e);
        }
    }

    private void sendFetchAck(PlayerSession session, int mailType, int mailIndex, int ret) {
        try {
            Msgmail.PB_MailAckInfo ack = Msgmail.PB_MailAckInfo.newBuilder()
                    .setMailType(mailType).setMailIndex(mailIndex).setRet(ret).build();
            Msgmail.PB_SCFetchMailAck resp = Msgmail.PB_SCFetchMailAck.newBuilder()
                    .addAskInfo(ack).build();
            Emitters.emit(session, 9506, resp.toByteArray());
        } catch (Exception e) {
            log.error("[Mail] sendFetchAck failed", e);
        }
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private boolean getBool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return Boolean.TRUE.equals(v);
    }
}
