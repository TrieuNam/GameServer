package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.UserServiceFeignClient;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleExpChangeHandler implements GameMessageHandler {
    @Autowired
    private UserServiceFeignClient userService;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            String roleId = (String) session.getAttributes().get("roleId");
            Long exp = userService.getRoleExp(roleId);

            Msgrole.PB_SCRoleExpChange resp = Msgrole.PB_SCRoleExpChange.newBuilder()
                    .setCurExp(exp == null ? 0L : exp)
                    .setChangeExp(100) // Nếu muốn tính chênh lệch exp thì lấy thêm param hoặc cache
                    .build();
            sendResponse(session, 1402, resp.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() { return 1402; }
}
