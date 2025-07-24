package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.UserSettingsDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RoleSystemSetHandler implements GameMessageHandler {
    @Autowired
    private UserServiceFeignClient settingsFeignClient;

    @Autowired
    private WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // Parse payload ra PB_CSRoleSystemSetReq (client gửi protobuf)
            Msgrole.PB_CSRoleSystemSetReq reqPb = Msgrole.PB_CSRoleSystemSetReq.parseFrom(payload);

            // Lấy userKey từ session hoặc payload
            String userKey = (String) session.getAttributes().get("userKey");

            // Chuyển protobuf system_set_list thành JSON string hoặc DTO tuỳ service phía dưới dùng
            // Ví dụ: lưu JSON Array, hoặc từng field (ở đây demo với JSON)
            List<Map<String, Object>> settingsList = new ArrayList<>();
            for (Msgrole.PB_system_set set : reqPb.getSystemSetListList()) {
                Map<String, Object> m = new HashMap<>();
                m.put("systemSetType", set.getSystemSetType());
                m.put("systemSetParam", set.getSystemSetParam());
                settingsList.add(m);
            }
            String settingsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(settingsList);

            UserSettingsDto dto = new UserSettingsDto();
            dto.setUserKey(userKey);
            dto.setSettingsJson(settingsJson);

            settingsFeignClient.updateSettings(dto);

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    userKey,
                    "ROLE_SYSTEM_SET",
                    dto
            );
            // Thường không cần gửi resp/ack, hoặc gửi MsgId riêng nếu muốn
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1460;
    }
}