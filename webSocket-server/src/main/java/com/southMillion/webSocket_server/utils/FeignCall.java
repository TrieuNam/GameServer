package com.southMillion.webSocket_server.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;


@Slf4j
public final class FeignCall {
    private FeignCall(){}
    private static Scheduler VT = Schedulers.boundedElastic();

    @Component
    static class Injector {
        Injector(@org.springframework.beans.factory.annotation.Qualifier("feignVtScheduler") Scheduler s) {
            VT = s;
        }
    }

    public static <T> Mono<T> withToken(String token, String label, Supplier<T> blocking) {
        return Mono.fromCallable(() -> {
                    FeignTokenHolder.set(token);
                    try {
                        return blocking.get();
                    } finally {
                        FeignTokenHolder.clear();
                    }
                })
                .flatMap(Mono::justOrEmpty)
                .subscribeOn(VT);
    }
}