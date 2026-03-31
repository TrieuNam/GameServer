package com.SouthMillion.webSocket_server.service;

import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
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
        if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) return; // đã có rồi → bỏ qua
        String token = FeignTokenHolder.get();
        if (org.springframework.util.StringUtils.hasText(token)) {
            String auth = token.startsWith("Bearer ") ? token : "Bearer " + token; // <-- tránh double Bearer
            template.header(HttpHeaders.AUTHORIZATION, auth);

            // log gọn + xác thực có header
            String url = template.url();
            if (url.length() > 200) url = url.substring(0, 200) + "...";
            log.debug("[feign] add Authorization (len={}, startsWithBearer={}) for {} {}",
                    token.length(), token.startsWith("Bearer "), template.method(), url);
        } else {
            log.debug("[feign] NO token for {} {}", template.method(), template.url());
        }
    }
}