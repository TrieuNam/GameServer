package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.LimitCoreInfoDto;
import org.SouthMillion.dto.user.LimitCoreReqDto;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class LimitCoreHandler implements GameMessageHandler {

    @Autowired
    private UserServiceFeignClient limitCoreFeignClient;

    @Autowired
    private WebsocketEventProducer eventProducer; // Thêm vào

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgother.PB_CSLimitCoreReq req = Msgother.PB_CSLimitCoreReq.parseFrom(payload);

            LimitCoreReqDto dto = new LimitCoreReqDto();
            dto.setUserId(getUserIdFromSession(session));
            dto.setType(req.getType());
            dto.setP1(req.getP1());

            LimitCoreInfoDto respDto;
            String eventType;
            if (req.getType() == 0) {
                respDto = limitCoreFeignClient.updateCoreInfo(dto);
                eventType = "LIMIT_CORE_UPDATE";
            } else {
                respDto = limitCoreFeignClient.getCoreInfo(dto);
                eventType = "LIMIT_CORE_GET";
            }

            Msgother.PB_SCLimitCoreInfo.Builder builder = Msgother.PB_SCLimitCoreInfo.newBuilder();
            if (respDto.getCoreLevel() != null) {
                builder.addAllCoreLevel(respDto.getCoreLevel());
            }
            Msgother.PB_SCLimitCoreInfo resp = builder.build();

            sendResponse(session, 1468, resp.toByteArray());

            // Gửi event Kafka sau khi xử lý
            eventProducer.sendGameEvent(
                    String.valueOf(dto.getUserId()),
                    eventType,
                    respDto
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1467;
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
