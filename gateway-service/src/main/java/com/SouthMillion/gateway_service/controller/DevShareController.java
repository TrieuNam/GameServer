package com.SouthMillion.gateway_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dev")
public class DevShareController {

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Gateway health OK");
    }
}
