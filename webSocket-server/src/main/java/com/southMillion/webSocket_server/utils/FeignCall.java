package com.southMillion.webSocket_server.utils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

public final class FeignCall {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeignCall.class);

    public static <T> Mono<T> withToken(String token, Supplier<T> blockingFeignCall) {
        return withToken(token, "unnamed", blockingFeignCall);
    }

    public static <T> Mono<T> withToken(String token, String tag, Supplier<T> blockingFeignCall) {
        return Mono.fromCallable(() -> {
                    FeignTokenHolder.set(token);
                    log.debug("[feignCall:{}] start on {}", tag, Thread.currentThread().getName());
                    try {
                        return blockingFeignCall.get();
                    } finally {
                        FeignTokenHolder.clear();
                        log.debug("[feignCall:{}] end", tag);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}