package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.net.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HandlerRegistry {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HandlerRegistry.class);

    private final Map<Integer, MessageHandler> map = new ConcurrentHashMap<>();

    public HandlerRegistry(List<MessageHandler> handlers) {
        for (MessageHandler h : handlers) {
            for (int id : h.interests()) {
                MessageHandler old = map.put(id, h);
                if (old != null && old != h) {
                    log.warn("MsgId {} overridden: {} -> {}",
                            id, old.getClass().getSimpleName(), h.getClass().getSimpleName());
                }
            }
        }
        log.info("HandlerRegistry: {} handlers, {} MsgIds", handlers.size(), map.size());
    }

    public Optional<MessageHandler> find(int msgId) {
        return Optional.ofNullable(map.get(msgId));
    }
}