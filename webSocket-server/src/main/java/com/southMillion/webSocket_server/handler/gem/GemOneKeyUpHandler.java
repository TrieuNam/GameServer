package com.southMillion.webSocket_server.handler.gem;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.gem.GemFeignClient;
import org.SouthMillion.dto.item.gem.GemDrawingDTO;
import org.SouthMillion.dto.item.gem.GemstoneDTO;
import org.SouthMillion.mapper.box.GemProtoMapper;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GemOneKeyUpHandler implements GameMessageHandler {
    @Autowired
    private GemFeignClient gemFeign;
    // (nếu có thêm service khác, inject thêm ở đây)

    @Override
    public int getMsgId() {
        return 1666;
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_CSGemOneKeyUpLevelReq req;
        try {
            req = Msgother.PB_CSGemOneKeyUpLevelReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Long userId = getUserIdFromSession(session);
        List<Integer> itemIds = req.getItemIdsList();
        gemFeign.oneKeyUpgradeGem(userId, itemIds);

        List<GemDrawingDTO> drawings = gemFeign.getAllDrawingsByUser(userId);
        Msgother.PB_SCGemInfo.Builder resp = Msgother.PB_SCGemInfo.newBuilder();
        resp.setDrawingId(-1);
        for (GemDrawingDTO dto : drawings) {
            resp.addDrawingList(GemProtoMapper.toProto(dto));
        }
        sendResponse(session, 1661, resp.build().toByteArray());
    }
    private Long getUserIdFromSession(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        return val != null ? Long.parseLong(val.toString()) : null;
    }
}
