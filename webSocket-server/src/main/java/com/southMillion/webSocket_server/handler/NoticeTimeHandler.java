package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.NoticeTimeReqDto;
import org.SouthMillion.dto.user.NoticeTimeRespDto;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class NoticeTimeHandler implements GameMessageHandler {

    @Autowired
    private UserServiceFeignClient noticeTimeFeignClient;

    @Autowired
    private WebsocketEventProducer eventProducer; // Bổ sung inject

    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // Parse payload từ client
            Msgother.PB_CSNoticeTimeReq req = Msgother.PB_CSNoticeTimeReq.parseFrom(payload);

            // Convert Protobuf -> DTO
            NoticeTimeReqDto dto = new NoticeTimeReqDto();
            dto.setUserId(getUserIdFromSession(session));
            dto.setType(req.getType());
            dto.setParam(req.getParam());

            // Gọi FeignClient
            NoticeTimeRespDto respDto;
            String eventType;
            if (req.getType() == 0) {
                respDto = noticeTimeFeignClient.getNoticeTime(dto);
                eventType = "NOTICE_TIME_GET";
            } else {
                respDto = noticeTimeFeignClient.setNoticeTime(dto);
                eventType = "NOTICE_TIME_SET";
            }

            // Convert DTO -> Protobuf
            Msgother.PB_SCNoticeTimeRet resp = Msgother.PB_SCNoticeTimeRet.newBuilder()
                    .setNoticeTime(respDto.getNoticeTime())
                    .build();

            // Gửi về client
            sendResponse(session, 1465, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
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
        return 1464;
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}