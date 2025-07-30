package com.southMillion.webSocket_server.handler.gem;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.gem.GemFeignClient;
import org.SouthMillion.dto.item.gem.GemDrawingDTO;
import org.SouthMillion.dto.item.gem.GemstoneDrawingDTO;
import org.SouthMillion.mapper.box.GemProtoMapper;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GemInfoHandler implements GameMessageHandler {
    @Autowired
    private GemFeignClient gemFeign;

    @Override
    public int getMsgId() {
        return 1661;
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_CSGemReq req;
        try {
            req = Msgother.PB_CSGemReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        int drawingId = req.getParam1();
        Long userId = getUserIdFromSession(session);
        Msgother.PB_SCGemInfo.Builder resp = Msgother.PB_SCGemInfo.newBuilder();
        resp.setDrawingId(drawingId);

        if (drawingId == -1) {
            List<GemDrawingDTO> drawings = gemFeign.getAllDrawingsByUser(userId);
            for (GemDrawingDTO dto : drawings) {
                resp.addDrawingList(GemProtoMapper.toProto(dto));
            }
        } else {
            GemDrawingDTO draw = gemFeign.getDrawingByUser(userId, (long)drawingId);
            if (draw != null) {
                resp.addDrawingList(GemProtoMapper.toProto(draw));
            }
        }
        sendResponse(session, 1661, resp.build().toByteArray());
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        return val != null ? Long.parseLong(val.toString()) : null;
    }
}
