package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.utils.FeignTokenHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeignAuthInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        String token = FeignTokenHolder.get();
        if (org.springframework.util.StringUtils.hasText(token)) {
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            // Chỉ log độ dài + vài ký tự đầu/cuối để tránh lộ token
            log.debug("[feign] add Authorization (len={}) for {} {}",
                    token.length(), template.method(), template.url());
        } else {
            log.debug("[feign] NO token for {} {}", template.method(), template.url());
        }
    }
}