package com.SouthMillion.webSocket_server.handler;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles lazy loading of non-essential game modules on demand.
 *
 * <p>When the player opens a specific UI panel (e.g., friends list, guild, activities),
 * the client sends CS_FEATURE_DATA_REQ (msgId 1453) with the module name. This handler
 * looks up the corresponding {@link LazyLoadHandler} and invokes its data loading logic.
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Reduces initial login time by 1-2 seconds (removes 4-6 handlers from bootstrap)</li>
 *   <li>Data loads only when needed (not all players open all features)</li>
 *   <li>More responsive initial login experience</li>
 * </ul>
 *
 * <p><b>Protocol:</b>
 * <pre>
 * CS 1453 PB_CSFeatureDataReq { module_name: "activity" }
 * → Server loads activity data via OpenServerActivityHandler.loadOnDemand()
 * → SC 2161, 2163, 2165, 2167 (activity data messages)
 * </pre>
 *
 * @see LazyLoadHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LazyDataRequestHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LazyDataRequestHandler.class);

    private final List<LazyLoadHandler> lazyHandlers;
    private final Map<String, LazyLoadHandler> handlerRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (LazyLoadHandler handler : lazyHandlers) {
            String moduleName = handler.getModuleName();
            if (moduleName != null && !moduleName.isBlank()) {
                handlerRegistry.put(moduleName.toLowerCase().trim(), handler);
                log.info("[lazy-load] Registered module: {} → {}", moduleName, handler.getClass().getSimpleName());
            }
        }
        log.info("[lazy-load] Initialized with {} lazy-loadable modules", handlerRegistry.size());
    }

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_FEATURE_DATA_REQ}; // 1453
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (ps.getRoleId() == null) {
            log.debug("[lazy-load] Ignored request from session without roleId");
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    try {
                        Msgrole.PB_CSFeatureDataReq req = Msgrole.PB_CSFeatureDataReq.parseFrom(payload);
                        if (!req.hasModuleName()) {
                            return "";
                        }
                        return new String(req.getModuleName().toByteArray(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.warn("[lazy-load] Parse error for roleId={}: {}", ps.getRoleId(), e.toString());
                        return "";
                    }
                })
                .flatMap(moduleName -> {
                    if (moduleName == null || moduleName.isBlank()) {
                        log.debug("[lazy-load] Empty module name from roleId={}", ps.getRoleId());
                        return Mono.empty();
                    }

                    String moduleKey = moduleName.toLowerCase().trim();
                    LazyLoadHandler handler = handlerRegistry.get(moduleKey);

                    if (handler == null) {
                        log.warn("[lazy-load] Unknown module '{}' requested by roleId={}", moduleName, ps.getRoleId());
                        return Mono.empty();
                    }

                    log.info("[lazy-load] Loading module '{}' for roleId={} via {}",
                            moduleName, ps.getRoleId(), handler.getClass().getSimpleName());

                    return handler.loadOnDemand(ps)
                            .doOnSuccess(v -> log.debug("[lazy-load] Successfully loaded '{}' for roleId={}",
                                    moduleName, ps.getRoleId()))
                            .onErrorResume(e -> {
                                log.warn("[lazy-load] Failed to load '{}' for roleId={}: {}",
                                        moduleName, ps.getRoleId(), e.toString());
                                return Mono.empty();
                            });
                });
    }
}
