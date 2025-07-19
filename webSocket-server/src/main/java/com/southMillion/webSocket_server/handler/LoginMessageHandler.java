package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.UserServiceFeignClient;
import org.SouthMillion.dto.user.LoginVerify;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class LoginMessageHandler implements GameMessageHandler {
    @Autowired
    private  UserServiceFeignClient userServiceFeignClient;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // 1. Giải mã protobuf login
            Msglogin.PB_CSLoginToAccount login = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
            System.out.println("Login: " + login.getPname() + ", device: " + login.getDeviceId());

            // 2. Gọi user-service xác thực
            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("username", login.getPname());
            reqBody.put("login_str", login.getLoginStr());
            reqBody.put("device_id", login.getDeviceId());
            // ... bạn có thể bổ sung thêm các field khác nếu cần

            LoginVerify result = userServiceFeignClient.login(
                    login.getPlatSpid() + "",
                    login.getDeviceId(),
                    login.getPname(),
                    Integer.toUnsignedLong(login.getLoginTime()),  // phải đúng kiểu Long
                    login.getLoginStr()    // nếu sign là loginStr
            );

            // 3. Xử lý kết quả trả về (giả sử field "success" và "forbid_time")
            boolean loginSuccess = result != null && result.getRet() == 0;
            int forbidTime = 0; // Không có trường này trong user, set mặc định là 0 (sẽ fix sau)

            if (loginSuccess) {
                // Trả về login thành công (msgId 7000)
                Msglogin.PB_SCLoginToAccount.Builder respBuilder = Msglogin.PB_SCLoginToAccount.newBuilder()
                        .setResult(0) // 0: OK, bạn có thể map mã khác
                        .setForbidTime(forbidTime);
                Msglogin.PB_SCLoginToAccount response = respBuilder.build();
                sendResponse(session, 7000, response.toByteArray());
            } else {
                // Trả về lỗi KEY (msgId 7004)
                Msglogin.PB_SCAccountKeyError.Builder errBuilder = Msglogin.PB_SCAccountKeyError.newBuilder();
                Msglogin.PB_SCAccountKeyError errResp = errBuilder.build();
                sendResponse(session, 7004, errResp.toByteArray());
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Nếu có lỗi parse/gọi service, trả về lỗi KEY
            try {
                Msglogin.PB_SCAccountKeyError.Builder errBuilder = Msglogin.PB_SCAccountKeyError.newBuilder();
                Msglogin.PB_SCAccountKeyError errResp = errBuilder.build();
                sendResponse(session, 7004, errResp.toByteArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public int getMsgId() {
        return 7056;
    }


}