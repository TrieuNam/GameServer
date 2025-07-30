package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.user.UserGameFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import com.southMillion.webSocket_server.utils.WebSocketSessionHolder;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class LoginHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msglogin.PB_CSLoginToAccount req;
        try {
            req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
        } catch (Exception e) {
            sendResponse(session, 7000, Msglogin.PB_SCLoginToAccount.newBuilder().setResult(99).build().toByteArray());

            return;
        }


        // Lấy đầy đủ tham số từ req
        String platSpid = req.hasPlatSpid() ? String.valueOf(req.getPlatSpid()) : null;
        String device = req.hasDeviceId() ? req.getDeviceId() : null;
        String userId = req.hasPname() ? req.getPname() : null;
        Long timestamp = req.hasLoginTime() ? (long) req.getLoginTime() : null;
        String sign = req.hasLoginStr() ? req.getLoginStr() : null;

        // Gọi service REST qua Feign
        LoginResponseDTO respDTO = userFeign.login(platSpid, device, userId, timestamp, sign);
        if(sign != null ){
            if(session != null){
                // --- Lưu token vào WebSocketSession ---
                session.getAttributes().put("jwtToken",sign);
                WebSocketSessionHolder.setCurrentSession(session);
            }
        }

        // Đăng nhập thành công → lấy roleId đầu tiên của user (nếu có)
        if (respDTO.getRet() == 0 && respDTO.getUser() != null) {

            List<Integer> roleIds = userFeign.getRoleIds(userId);
            if (roleIds != null && !roleIds.isEmpty()) {
                SessionUtils.setRoleId(session, roleIds.get(0));
            }
            SessionUtils.setUserId(session, userId);
            String sessionId = userId + "_" + System.currentTimeMillis();
            SessionUtils.setSessionId(session,sessionId);
        }

        // Map LoginResponseDTO.result sang PB_SCLoginToAccount.result
        Msglogin.PB_SCLoginToAccount.Builder resp = Msglogin.PB_SCLoginToAccount.newBuilder()
                .setResult(respDTO.getRet()); // hoặc respDTO.getResult(), tùy tên field bạn đang dùng

        // Nếu bị ban, truyền forbidTime, nếu không thì không set (proto optional)
        if (respDTO.getRet() == 2 && resp.getForbidTime() != 0) {
            resp.setForbidTime(resp.getForbidTime());
        }

        sendResponse(session, 7000, resp.build().toByteArray());

    }

    @Override
    public int getMsgId() {
        return 7056;
    }
}