package com.SouthMillion.webSocket_server.handler.scroll;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.grpc.ScrollGrpcClient;
import org.SouthMillion.proto.scroll.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgscroll.Msgscroll;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Handles scroll/lottery system operations.
 *
 * Proto: PB_CSScrollReq (2170) — req_type + param1
 * Responses:
 *   2171 PB_SCScrollInfo     — scroll system info (free_num, bao_di_num)
 *   2172 PB_SCScrollListInfo — list of all scrolls
 *   2173 PB_SCScrollOneInfo  — single scroll update (draw result)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrollHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScrollHandler.class);

    private final ScrollGrpcClient scrollGrpcClient;

    private static final int OP_GET_INFO  = 0;
    private static final int OP_DRAW      = 1;
    private static final int OP_GET_LIST  = 2;

    @Override
    public int[] interests() {
        return new int[]{2170};
    }

    /** Goi sau login: day thong tin scroll (2171) va danh sach scroll (2172) ve client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleIdStr = session.getRoleId();
        if (roleIdStr == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = roleIdStr;
                sendScrollInfo(session, roleId);
                sendListInfo(session, roleId);
            } catch (NumberFormatException e) {
                log.warn("[Scroll] pushAll: roleId khong hop le={}", roleIdStr);
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgscroll.PB_CSScrollReq req = Msgscroll.PB_CSScrollReq.parseFrom(payload);
                int reqType = req.hasReqType() ? req.getReqType() : OP_GET_INFO;
                int param1  = req.hasParam1()  ? req.getParam1()  : 1;
                Long roleId = session.getRoleId();

                log.debug("[Scroll] reqType={}, param1={}, roleId={}", reqType, param1, roleId);

                switch (reqType) {
                    case OP_DRAW     -> handleDraw(session, roleId, param1);
                    case OP_GET_LIST -> sendListInfo(session, roleId);
                    default          -> sendScrollInfo(session, roleId);
                }
            } catch (Exception e) {
                log.error("[Scroll] Error for roleId={}", session.getRoleId(), e);
                sendScrollInfoEmpty(session);
            }
        });
    }

    private void handleDraw(PlayerSession session, Long roleId, int count) {
        scrollGrpcClient.draw(roleId, count > 0 ? count : 1);
        sendScrollInfo(session, roleId);
    }

    private void sendScrollInfo(PlayerSession session, Long roleId) {
        try {
            ScrollMetaResponse resp = scrollGrpcClient.getMeta(roleId);
            Msgscroll.PB_SCScrollInfo.Builder builder = Msgscroll.PB_SCScrollInfo.newBuilder();
            if (resp.getSuccess() && resp.hasMeta()) {
                builder.setFreeNum(resp.getMeta().getFreeNum());
                builder.setBaoDiNum(resp.getMeta().getBaoDiNum());
            }
            Emitters.emit(session, 2171, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Scroll] sendScrollInfo failed", e);
            sendScrollInfoEmpty(session);
        }
    }

    private void sendListInfo(PlayerSession session, Long roleId) {
        try {
            GetListResponse resp = scrollGrpcClient.getList(roleId);
            Msgscroll.PB_SCScrollListInfo.Builder builder = Msgscroll.PB_SCScrollListInfo.newBuilder();
            for (ScrollItemData item : resp.getItemsList()) {
                Msgscroll.PB_ScrollData.Builder sd = Msgscroll.PB_ScrollData.newBuilder();
                sd.setIndex(item.getScrollIndex());
                sd.setItemId(item.getItemId());
                sd.setLevel(item.getLevel());
                sd.setWearMark(item.getWearMark());
                sd.setParam(item.getParam());
                builder.addScrollList(sd.build());
            }
            Emitters.emit(session, 2172, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Scroll] sendListInfo failed", e);
            sendScrollInfoEmpty(session);
        }
    }

    private void sendScrollInfoEmpty(PlayerSession session) {
        try {
            Emitters.emit(session, 2171, Msgscroll.PB_SCScrollInfo.newBuilder().setFreeNum(0).setBaoDiNum(0).build().toByteArray());
        } catch (Exception ignored) {}
    }
}
