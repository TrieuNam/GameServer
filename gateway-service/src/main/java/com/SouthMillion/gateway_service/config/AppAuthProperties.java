package com.SouthMillion.gateway_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {
    private List<String> whitelist = List.of();
    private Ws ws = new Ws();
    private String sessionServiceId = "session-service";
    private String introspectPath = "/internal/session/introspect";
    private String introspectMethod = "POST";

    public static class Ws {
        private String queryTokenKey = "token";
        private List<String> headerTokenKeys = List.of("Authorization", "Sec-WebSocket-Protocol");
        public String getQueryTokenKey() { return queryTokenKey; }
        public void setQueryTokenKey(String k) { this.queryTokenKey = k; }
        public List<String> getHeaderTokenKeys() { return headerTokenKeys; }
        public void setHeaderTokenKeys(List<String> v) { this.headerTokenKeys = v; }
    }

    public List<String> getWhitelist() { return whitelist; }
    public void setWhitelist(List<String> whitelist) { this.whitelist = whitelist; }
    public Ws getWs() { return ws; }
    public void setWs(Ws ws) { this.ws = ws; }
    public String getSessionServiceId() { return sessionServiceId; }
    public void setSessionServiceId(String sessionServiceId) { this.sessionServiceId = sessionServiceId; }
    public String getIntrospectPath() { return introspectPath; }
    public void setIntrospectPath(String introspectPath) { this.introspectPath = introspectPath; }
    public String getIntrospectMethod() { return introspectMethod; }
    public void setIntrospectMethod(String introspectMethod) { this.introspectMethod = introspectMethod; }
}