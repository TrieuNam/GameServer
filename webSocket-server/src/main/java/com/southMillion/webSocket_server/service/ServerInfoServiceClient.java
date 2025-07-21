package com.southMillion.webSocket_server.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "serverInfo-service")
public interface ServerInfoServiceClient {

    @GetMapping("/api/server-info/real-start-time")
    long getServerRealStartTime();

    @GetMapping("/api/server-info/combine-time")
    long getServerCombineTime();
}
