package com.southMillion.webSocket_server.handler;

import com.google.protobuf.ByteString;
import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.RoleDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleInfoHandler implements GameMessageHandler {
    @Autowired
    private UserServiceFeignClient userService;

    @Autowired
    private WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            String roleId = (String) session.getAttributes().get("roleId");
            RoleDto role = userService.getRoleInfo(roleId);

            Msgrole.PB_RoleInfo.Builder pbRoleInfo = Msgrole.PB_RoleInfo.newBuilder()
                    .setRoleId(Integer.parseInt(role.getRoleId()))
                    .setName(ByteString.copyFromUtf8(role.getRoleName()))
                    .setLevel(role.getLevel())
                    .setCap(role.getCap() == null ? 0L : role.getCap());
            // Set thêm field nếu muốn

            Msgrole.PB_SCRoleInfoAck.Builder builder = Msgrole.PB_SCRoleInfoAck.newBuilder()
                    .setCurExp(role.getExp() == null ? 0L : role.getExp())
                    .setCreateTime(role.getCreateTime() == null ? 0L : role.getCreateTime())
                    .setRoleinfo(pbRoleInfo);

            // Nếu có attrs hoặc appearance, mapping thêm...

            sendResponse(session, 1400, builder.build().toByteArray());

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    roleId,
                    "ROLE_INFO",
                    role
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1400;
    }
}
