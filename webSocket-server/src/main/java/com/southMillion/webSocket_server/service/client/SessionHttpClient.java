package com.southMillion.webSocket_server.service.client;


import org.SouthMillion.dto.session.IntrospectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "session-service", path = "/internal/session")
public interface SessionHttpClient {
    @PostMapping("/introspect")
    IntrospectResponse introspect(@RequestBody String accessToken);
}