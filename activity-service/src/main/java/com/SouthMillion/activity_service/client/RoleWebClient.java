package com.SouthMillion.activity_service.client;

import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;

/**
 * Non-blocking WebClient-based client for role-service API calls.
 * Replaces synchronous RoleFeign to eliminate request thread blocking.
 * All operations are fully asynchronous using Project Reactor Mono.
 */
@Slf4j
@Component
public class RoleWebClient {

    private final WebClient webClient;
    
    @Value("${role-service.url:http://role-service/api/role}")
    private String roleServiceUrl;
    
    @Value("${role-service.timeout:5000}")
    private long timeoutMs;

    public RoleWebClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Asynchronously fetch role details by roleId.
     * 
     * @param roleId the role identifier
     * @return Mono<RoleDTOs.RoleResp> that emits role details or completes empty on 404
     */
    public Mono<RoleDTOs.RoleResp> getRoleDetail(Long roleId) {
        return webClient.get()
                .uri(roleServiceUrl + "/{roleId}", roleId)
                .retrieve()
                .bodyToMono(RoleDTOs.RoleResp.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .filter(throwable -> isRetryable(throwable)))
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.debug("Role not found for roleId: {}", roleId);
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.error("Failed to fetch role detail for roleId: {}", roleId, ex);
                    return Mono.error(ex);
                });
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException.ServiceUnavailable ||
               throwable instanceof WebClientResponseException.GatewayTimeout ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
}