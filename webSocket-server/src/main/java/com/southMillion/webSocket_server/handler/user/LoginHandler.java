package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.request.LoginRequestDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class LoginHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msglogin.PB_CSLoginToAccount req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
            LoginRequestDTO dto = new LoginRequestDTO();
            dto.setLoginTime((int) req.getLoginTime());
            dto.setLoginStr(req.getLoginStr());
            dto.setPname(req.getPname());
            dto.setServer(req.getServer());
            dto.setPlatSpid(req.getPlatSpid());
            dto.setDeviceId(req.getDeviceId());
            LoginResponseDTO resp = userFeign.login(dto);

            Msglogin.PB_SCLoginToAccount.Builder respPb = Msglogin.PB_SCLoginToAccount.newBuilder()
                    .setResult(resp.getResult())
                    .setForbidTime(resp.getForbidTime());
            sendResponse(session, 7000, respPb.build().toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 7056;
    }
}