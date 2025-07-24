package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.UserSettingsDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleSystemSetInfoHandler implements GameMessageHandler {
    @Autowired
    private UserServiceFeignClient settingsFeignClient;

    @Autowired
    private WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            String userKey = (String) session.getAttributes().get("userKey");

            // Gọi user-service lấy settings
            UserSettingsDto dto = settingsFeignClient.getSettings(userKey);

            // Parse settingsJson thành list PB_system_set
            List<Msgrole.PB_system_set> pbList = new ArrayList<>();
            if (dto != null && dto.getSettingsJson() != null) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> list = objectMapper.readValue(dto.getSettingsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : list) {
                    int type = (int) item.get("systemSetType");
                    int param = (int) item.get("systemSetParam");
                    pbList.add(Msgrole.PB_system_set.newBuilder()
                            .setSystemSetType(type)
                            .setSystemSetParam(param)
                            .build());
                }
            }

            Msgrole.PB_SCRoleSystemSetInfo resp = Msgrole.PB_SCRoleSystemSetInfo.newBuilder()
                    .addAllSystemSetList(pbList)
                    .build();

            sendResponse(session, 1461, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    userKey,
                    "ROLE_SYSTEM_SET_INFO",
                    dto
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1461;
    }
}
