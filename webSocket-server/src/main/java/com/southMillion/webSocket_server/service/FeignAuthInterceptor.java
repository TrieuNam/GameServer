package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.utils.FeignTokenUtils;
import com.southMillion.webSocket_server.utils.WebSocketSessionHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // Thêm Authorization header cho mọi request Feign
        if(WebSocketSessionHolder.getCurrentSession() != null){
            String token = FeignTokenUtils.getCurrentUserToken(WebSocketSessionHolder.getCurrentSession());
            if (token != null) {
                template.header("Authorization", "Bearer " + token);
            }
        }
    }
}