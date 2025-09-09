package com.southMillion.webSocket_server.utils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;


public final class FeignCall {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeignCall.class);
    private static reactor.core.scheduler.Scheduler VT = reactor.core.scheduler.Schedulers.boundedElastic();

    // Inject scheduler qua static setter (đơn giản, không “tạo” scheduler mới mỗi lần)
    @Component
    static class Holder {
        Holder(@Qualifier("feignVtScheduler") reactor.core.scheduler.Scheduler s) {
            VT = s;
        }
    }

    public static <T> Mono<T> withToken(String token, String tag, Supplier<T> blockingFeignCall) {
        return Mono.fromCallable(() -> {
                    FeignTokenHolder.set(token);
                    log.debug("[feignCall:{}] start on {}", tag, Thread.currentThread().getName());
                    try {
                        return blockingFeignCall.get();
                    } finally {
                        FeignTokenHolder.clear();
                        log.debug("[feignCall:{}] end");
                    }
                })
                .flatMap(Mono::justOrEmpty)
                .subscribeOn(VT);
    }

    public static <T> Mono<T> withToken(String token, Supplier<T> call) {
        return withToken(token, "unnamed", call);
    }
}