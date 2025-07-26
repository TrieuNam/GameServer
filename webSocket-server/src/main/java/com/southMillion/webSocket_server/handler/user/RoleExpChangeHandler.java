package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.RoleDTO;
import org.SouthMillion.dto.user.RoleExpChangeDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleExpChangeHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {

        Integer uid = getUserIdFromSession(session);
        // Lấy exp hiện tại của user
        RoleDTO role = userFeign.getRoleInfo(uid);  // hoặc method khác lấy exp
        // Mapping sang response (không có expChange, chỉ curExp)
        Msgrole.PB_SCRoleExpChange resp = Msgrole.PB_SCRoleExpChange.newBuilder()
                .setChangeExp(0)                   // Không thay đổi
                .setCurExp(role.getCurExp() != null ? role.getCurExp() : 0)
                .build();
        sendResponse(session, 1402, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1402;
    }
}