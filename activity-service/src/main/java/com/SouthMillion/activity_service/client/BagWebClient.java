package com.SouthMillion.activity_service.client;

import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;

/**
 * Non-blocking WebClient-based client for bag-service API calls.
 * Replaces synchronous BagFeign to eliminate request thread blocking.
 * All operations are fully asynchronous using Project Reactor Mono.
 */
@Slf4j
@Component
public class BagWebClient {

    private final WebClient webClient;
    
    @Value("${bag-service.url:http://bag-service/api/bag}")
    private String bagServiceUrl;
    
    @Value("${bag-service.timeout:5000}")
    private long timeoutMs;

    public BagWebClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Asynchronously fetch bag details by bagId.
     * 
     * @param bagId the bag identifier
     * @return Mono<BagDTOs.BagResp> that emits bag details or completes empty on 404
     */
    public Mono<BagDTOs.BagResp> getBagDetail(Long bagId) {
        return webClient.get()
                .uri(bagServiceUrl + "/{bagId}", bagId)
                .retrieve()
                .bodyToMono(BagDTOs.BagResp.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .filter(throwable -> isRetryable(throwable)))
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.debug("Bag not found for bagId: {}", bagId);
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.error("Failed to fetch bag detail for bagId: {}", bagId, ex);
                    return Mono.error(ex);
                });
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException.ServiceUnavailable ||
               throwable instanceof WebClientResponseException.GatewayTimeout ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
}