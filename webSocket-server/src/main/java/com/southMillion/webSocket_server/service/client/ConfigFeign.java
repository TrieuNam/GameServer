package com.southMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "config-service")
public interface ConfigFeign {
    @GetMapping(value = "/config/gameworld/logicconfig/roleexp.json",
            consumes = MediaType.ALL_VALUE)
    ResponseEntity<byte[]> roleExpRaw();
}