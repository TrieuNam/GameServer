package com.southMillion.webSocket_server.handler.gem;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.gem.GemFeignClient;
import org.SouthMillion.dto.item.gem.GemstoneDTO;
import org.SouthMillion.mapper.box.GemProtoMapper;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GemOpHandler implements GameMessageHandler {
    @Autowired
    private GemFeignClient gemFeign;

    @Override
    public int getMsgId() {
        return 1660;
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_CSGemReq req;

        try {
            req = Msgother.PB_CSGemReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        // Xử lý dựa trên op_type (tuỳ business của bạn)
        if (req.getOpType() == 1) { // Ví dụ: Lấy toàn bộ gem
            List<GemstoneDTO> list = gemFeign.getAllGems();
            Msgother.PB_SCGemInfo.Builder resp = Msgother.PB_SCGemInfo.newBuilder();
            resp.setDrawingId(-1);

            // Mapping list DTO sang Proto
            for (GemstoneDTO dto : list) {
                resp.addDrawingList(GemProtoMapper.toProto(dto));
            }
            sendResponse(session, 1661, resp.build().toByteArray());
        } else {
            // Các op_type khác, bạn tự mở rộng

        }
    }
}
