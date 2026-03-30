package com.SouthMillion.globalserver_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Global Server Controller
 * Coordinates cross-server operations: server registration, heartbeat,
 * online player tracking, and global status queries.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/global")
public class GlobalServerController {

    private static final String KEY_SERVER_PREFIX  = "gs:server:";
    private static final String KEY_SERVERS_SET    = "gs:servers";
    private static final String KEY_ONLINE_PREFIX  = "gs:online:";
    private static final int    SERVER_TTL_SEC     = 60;  // server heartbeat TTL

    private final StringRedisTemplate redis;

    /** GET /api/global/status — service health + summary */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Set<String> servers = redis.opsForSet().members(KEY_SERVERS_SET);
        int serverCount = servers == null ? 0 : servers.size();

        return Map.of(
                "service", "globalserver-service",
                "status", "UP",
                "timestamp", Instant.now().getEpochSecond(),
                "registeredServers", serverCount
        );
    }

    /**
     * POST /api/global/server/register
     * Game servers call this to register themselves on startup.
     * Body: { "serverId": "ws-1", "host": "10.0.0.1", "port": 8094, "type": "websocket" }
     */
    @PostMapping("/server/register")
    public Map<String, Object> registerServer(@RequestBody Map<String, Object> body) {
        String serverId = String.valueOf(body.get("serverId"));
        log.info("[GlobalServer] Registering server: {}", serverId);

        String key = KEY_SERVER_PREFIX + serverId;
        Map<String, String> info = new HashMap<>();
        info.put("serverId", serverId);
        info.put("host",     String.valueOf(body.getOrDefault("host", "")));
        info.put("port",     String.valueOf(body.getOrDefault("port", "0")));
        info.put("type",     String.valueOf(body.getOrDefault("type", "unknown")));
        info.put("registeredAt", String.valueOf(Instant.now().getEpochSecond()));

        redis.opsForHash().putAll(key, info);
        redis.expire(key, SERVER_TTL_SEC, TimeUnit.SECONDS);
        redis.opsForSet().add(KEY_SERVERS_SET, serverId);

        return Map.of("registered", true, "serverId", serverId);
    }

    /**
     * POST /api/global/server/{serverId}/heartbeat
     * Keep server registration alive. Call every 30 seconds.
     */
    @PostMapping("/server/{serverId}/heartbeat")
    public Map<String, Object> heartbeat(@PathVariable String serverId,
                                         @RequestBody(required = false) Map<String, Object> stats) {
        String key = KEY_SERVER_PREFIX + serverId;
        if (!Boolean.TRUE.equals(redis.hasKey(key))) {
            return Map.of("ok", false, "error", "Server not registered");
        }
        redis.expire(key, SERVER_TTL_SEC, TimeUnit.SECONDS);

        if (stats != null) {
            stats.forEach((k, v) -> redis.opsForHash().put(key, k, String.valueOf(v)));
        }
        redis.opsForHash().put(key, "lastHeartbeat", String.valueOf(Instant.now().getEpochSecond()));

        return Map.of("ok", true, "serverId", serverId);
    }

    /** GET /api/global/servers — list all registered servers */
    @GetMapping("/servers")
    public Map<String, Object> listServers() {
        Set<String> serverIds = redis.opsForSet().members(KEY_SERVERS_SET);
        Map<String, Object> servers = new HashMap<>();
        if (serverIds != null) {
            for (String id : serverIds) {
                Map<Object, Object> info = redis.opsForHash().entries(KEY_SERVER_PREFIX + id);
                if (!info.isEmpty()) {
                    servers.put(id, info);
                } else {
                    // Entry expired — clean up set
                    redis.opsForSet().remove(KEY_SERVERS_SET, id);
                }
            }
        }
        return Map.of("servers", servers, "count", servers.size());
    }

    /**
     * POST /api/global/player/online
     * Mark a player as online on a specific server.
     * Body: { "roleId": "123", "serverId": "ws-1" }
     */
    @PostMapping("/player/online")
    public Map<String, Object> playerOnline(@RequestBody Map<String, Object> body) {
        String roleId   = String.valueOf(body.get("roleId"));
        String serverId = String.valueOf(body.get("serverId"));
        redis.opsForValue().set(KEY_ONLINE_PREFIX + roleId, serverId, 300, TimeUnit.SECONDS);
        return Map.of("ok", true);
    }

    /**
     * POST /api/global/player/offline
     * Remove a player from online tracking.
     */
    @PostMapping("/player/offline")
    public Map<String, Object> playerOffline(@RequestBody Map<String, Object> body) {
        String roleId = String.valueOf(body.get("roleId"));
        redis.delete(KEY_ONLINE_PREFIX + roleId);
        return Map.of("ok", true);
    }

    /**
     * GET /api/global/player/{roleId}/server
     * Get which server a player is currently on.
     */
    @GetMapping("/player/{roleId}/server")
    public Map<String, Object> playerServer(@PathVariable String roleId) {
        String serverId = redis.opsForValue().get(KEY_ONLINE_PREFIX + roleId);
        if (serverId == null) {
            return Map.of("online", false);
        }
        return Map.of("online", true, "serverId", serverId);
    }
}
