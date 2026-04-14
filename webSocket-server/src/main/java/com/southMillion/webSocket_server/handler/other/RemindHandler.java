package com.SouthMillion.webSocket_server.handler.other;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class RemindHandler implements MessageHandler {
    @Override
    public int[] interests() { return new int[]{}; } // No CS messages, server-push only

        @Override
        public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
            // No-op: RemindHandler is server-push only
            return Mono.empty();
        }

    public Mono<Void> pushAll(PlayerSession ps) {
        return Mono.fromCallable(() -> {
            // TODO: Aggregate red point counts from modules
            Map<String, Integer> remindCounts = computeAllRemindGroups(ps.getRoleId());
            // TODO: Build PB_SCRemindInfo proto
            // Emitters.emit(ps, MsgIds.SC_REMIND_INFO, proto);
            return null;
        }).timeout(Duration.ofSeconds(8)).then();
    }

    private Map<String, Integer> computeAllRemindGroups(Long roleId) {
        // TODO: Implement aggregation logic
        return Map.of();
    }
}
