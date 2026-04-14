package com.SouthMillion.webSocket_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ws.handler")
public class WsHandlerProperties {
    private long feignTimeoutMs = 1500;
    private long hardTimeoutMs = 8000;
    private long bootstrapTimeoutS = 12;

    public long getFeignTimeoutMs() {
        return feignTimeoutMs;
    }
    public void setFeignTimeoutMs(long feignTimeoutMs) {
        this.feignTimeoutMs = feignTimeoutMs;
    }
    public long getHardTimeoutMs() {
        return hardTimeoutMs;
    }
    public void setHardTimeoutMs(long hardTimeoutMs) {
        this.hardTimeoutMs = hardTimeoutMs;
    }
    public long getBootstrapTimeoutS() {
        return bootstrapTimeoutS;
    }
    public void setBootstrapTimeoutS(long bootstrapTimeoutS) {
        this.bootstrapTimeoutS = bootstrapTimeoutS;
    }
}
