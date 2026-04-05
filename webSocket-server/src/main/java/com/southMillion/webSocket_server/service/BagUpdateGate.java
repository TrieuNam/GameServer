package com.SouthMillion.webSocket_server.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-shot guard for bag UI refresh flows.
 *
 * Flow:
 * 1) arm(roleId) before starting a box/task bag refresh flow
 * 2) first successful UI bag update marks the gate as delivered
 * 3) the immediate duplicate update is skipped once and the gate is cleared
 *
 * If the first push fails, the gate stays undelivered so the fallback/second push is still allowed.
 */
@Component
public class BagUpdateGate {

    private static final long DEFAULT_TTL_MS = 1500L;
    private final ConcurrentHashMap<Long, GateState> gates = new ConcurrentHashMap<>();

    public void arm(Long roleId) {
        arm(roleId, DEFAULT_TTL_MS);
    }

    public void arm(Long roleId, long ttlMs) {
        if (roleId == null) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + Math.max(200L, ttlMs);
        gates.put(roleId, new GateState(new AtomicBoolean(false), expiresAt));
    }

    /** Mark that the first UI bag update for this armed flow already succeeded. */
    public void markDelivered(Long roleId) {
        if (roleId == null) {
            return;
        }
        GateState state = gates.get(roleId);
        if (state == null) {
            return;
        }
        if (state.isExpired()) {
            gates.remove(roleId, state);
            return;
        }
        state.delivered().set(true);
    }

    /**
     * @return true when this bag update should be emitted to the client, false when it is the
     *         duplicate second update of the currently armed flow and should be suppressed.
     */
    public boolean shouldEmit(Long roleId) {
        if (roleId == null) {
            return true;
        }
        GateState state = gates.get(roleId);
        if (state == null) {
            return true;
        }
        if (state.isExpired()) {
            gates.remove(roleId, state);
            return true;
        }
        if (state.delivered().compareAndSet(false, true)) {
            return true;
        }
        gates.remove(roleId, state);
        return false;
    }

    private record GateState(AtomicBoolean delivered, long expiresAt) {
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
