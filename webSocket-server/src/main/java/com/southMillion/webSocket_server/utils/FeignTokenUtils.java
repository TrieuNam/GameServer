package com.southMillion.webSocket_server.utils;

import feign.RequestTemplate;
import org.springframework.web.socket.WebSocketSession;

public class FeignTokenUtils {

    // Lấy token của user hiện tại (tùy bạn lưu ở đâu: session, Redis, JWT, ...)
    public static String getCurrentUserToken(WebSocketSession session) {
        // VD: bạn lưu trong session attribute, hoặc mapping userId->token
        Object token = session.getAttributes().get("jwtToken");
        return token != null ? token.toString() : null;
    }

    // Thêm token vào header Feign hoặc RestTemplate
    public static RequestTemplate addTokenHeader(RequestTemplate template, String token) {
        if (token != null && !token.isEmpty()) {
            template.header("Authorization", "Bearer " + token);
        }
        return template;
    }
}
