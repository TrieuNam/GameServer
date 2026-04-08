package com.SouthMillion.webSocket_server.handler.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.handler.activity.OpenServerActivityHandler;
import com.SouthMillion.webSocket_server.handler.analytics.AnalyticsHandler;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.handler.bag.BagHandler;
import com.SouthMillion.webSocket_server.handler.equip.EquipHandler;
import com.SouthMillion.webSocket_server.handler.shizhuang.ShiZhuangHandler;
import com.SouthMillion.webSocket_server.handler.pet.PetHandler;
import com.SouthMillion.webSocket_server.handler.mount.MountHandler;
import com.SouthMillion.webSocket_server.handler.rune.RuneHandler;
import com.SouthMillion.webSocket_server.handler.friend.FriendHandler;
import com.SouthMillion.webSocket_server.handler.mail.MailHandler;
import com.SouthMillion.webSocket_server.handler.guild.GuildHandler;
import com.SouthMillion.webSocket_server.handler.box.BoxHandler;
import com.SouthMillion.webSocket_server.handler.block.BlockHandler;
import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.handler.angel.AngelHandler;
import com.SouthMillion.webSocket_server.handler.gem.GemHandler;
import com.SouthMillion.webSocket_server.handler.lingzhu.LingZhuHandler;
import com.SouthMillion.webSocket_server.handler.shenqi.ShenQiHandler;
import com.SouthMillion.webSocket_server.handler.scroll.ScrollHandler;
import com.SouthMillion.webSocket_server.handler.pagoda.PagodaHandler;
import com.SouthMillion.webSocket_server.handler.arena.ArenaHandler;
import com.SouthMillion.webSocket_server.handler.wabao.WaBaoHandler;
import com.SouthMillion.webSocket_server.handler.starmap.StarMapHandler;
import com.SouthMillion.webSocket_server.handler.territory.TerritoryHandler;
import com.SouthMillion.webSocket_server.handler.escort.EscortHandler;
import com.SouthMillion.webSocket_server.handler.mainfb.MainFbHandler;
import com.SouthMillion.webSocket_server.handler.skill.SkillHandler;
import com.SouthMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.LoginSnapshotService;
import com.SouthMillion.webSocket_server.service.client.*;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import feign.FeignException;
import feign.RetryableException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import reactor.util.retry.Retry;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoginBootstrapHandler implements MessageHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ===== Feign / Registry =====
    private final SessionFeignClient sessionFeign;
    private final RoleFeign roleFeign;
    private final ActivityFeign activityFeign;
    private final InMemoryPlayerSessionRegistry registry;
    private final AnalyticsHandler analyticsHandler;
    private final LoginSnapshotService loginSnapshotService;

    /**
     * Virtual-thread scheduler — mỗi pushAll() chạy trên VT riêng để thật sự song song.
     * Dùng setter injection để @Qualifier hoạt động đúng với Lombok @RequiredArgsConstructor.
     */
    private Scheduler feignScheduler;

    @Autowired
    public void setFeignScheduler(@Qualifier("feignVtScheduler") Scheduler feignScheduler) {
        this.feignScheduler = feignScheduler;
    }

    // ===== Bootstrap handlers =====
    private final BagHandler bagHandler;
    private final EquipHandler equipHandler;
    private final ShiZhuangHandler shiZhuangHandler;
    private final PetHandler petHandler;
    private final MountHandler mountHandler;
    private final RuneHandler runeHandler;
    private final FriendHandler friendHandler;
    private final MailHandler mailHandler;
    private final GuildHandler guildHandler;
    private final BoxHandler boxHandler;
    private final OpenServerActivityHandler openServerActivityHandler;
    private final BlockHandler blockHandler;
    private final TaskHandler taskHandler;
    private final RoleServiceHandler roleServiceHandler;
    private final AngelHandler angelHandler;
    private final GemHandler gemHandler;
    private final LingZhuHandler lingZhuHandler;
    private final ShenQiHandler shenQiHandler;
    private final ScrollHandler scrollHandler;
    private final PagodaHandler pagodaHandler;
    private final ArenaHandler arenaHandler;
    private final WaBaoHandler waBaoHandler;
    private final StarMapHandler starMapHandler;
    private final TerritoryHandler territoryHandler;
    private final EscortHandler escortHandler;
    private final MainFbHandler mainFbHandler;
    private final SkillHandler skillHandler;

    // ===== Guard =====
    private static final Semaphore LOGIN_LIMITER = new Semaphore(128);
    /** Set to false via admin endpoint during maintenance to block new logins */
    private static volatile boolean WORLD_READY = true;
    @SuppressWarnings("unused")
    public static void setWorldReady(boolean ready) { WORLD_READY = ready; }

    // ===== Timing thresholds (ms) =====
    /** Log WARN nếu 1 service bootstrap mất hơn mức này */
    private static final long SLOW_WARN_MS  = 2_000;  // Lowered from 3s to detect issues earlier
    /** Log ERROR nếu 1 service bootstrap mất hơn mức này (gần timeout) */
    private static final long SLOW_ERROR_MS = 6_000;  // Lowered from 8s for earlier alerting
    /** Guard để client retry/reconnect không bắn full bootstrap trùng ngay sau 1450 đầu tiên. */
    private static final long BOOTSTRAP_REPLAY_GUARD_MS = 1_500;
    /** Fallback cho client cũ không gửi 1450: tự bootstrap nhẹ sau login ACK. */
    private static final long ALL_INFO_FALLBACK_DELAY_MS = 1_200;

    private final ConcurrentHashMap<Long, AtomicBoolean> bootstrapInFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> bootstrapLastStartMs = new ConcurrentHashMap<>();
    /** Freshly created roles should skip the first deferred non-core/cosmetic bootstrap wave. */
    private final ConcurrentHashMap<Long, Boolean> skipFreshRoleDeferredModulesOnce = new ConcurrentHashMap<>();

    // ===== Result codes (match client) =====
    private static final int LOGIN_OK                    = 0;
    private static final int LOGIN_ERR_MISSING_TOKEN     = 2;
    private static final int LOGIN_ERR_PARSE             = 4;
    private static final int LOGIN_ERR_SERVER_NOTREADY   = 11;
    private static final int LOGIN_ERR_SERVER_BUSY       = 12;
    private static final int LOGIN_ERR_FORBID            = 13;

    private static final int DISCONNECT_REASON_LOGIN_OTHER_PLACE = 1;

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_LOGIN_REQ, MsgIds.CS_ALL_INFO_REQ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (msgId == MsgIds.CS_ALL_INFO_REQ) {
            // CS_ALL_INFO_REQ (1450) vẫn được giữ để tương thích với client cũ,
            // nhưng bây giờ bootstrap đã được trigger ngay trong login flow.
            // Guard BOOTSTRAP_REPLAY_GUARD_MS sẽ ngăn load trùng lặp.
            if (ps.isLoggedIn() && ps.getRoleId() != null) {
                log.info("[1450] CS_ALL_INFO_REQ roleId={} — bootstrap may already be running from login", ps.getRoleId());
                triggerBootstrapOnAllInfo(ps, "CS_ALL_INFO_REQ");
            } else {
                log.warn("[1450] CS_ALL_INFO_REQ ignored — session not logged in ws={}", ps.getWs().getId());
            }
            return Mono.empty();
        }
        if (msgId != MsgIds.CS_LOGIN_REQ) return Mono.empty();
        return handleLogin(ps, payload);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN FLOW
    // ─────────────────────────────────────────────────────────────────────────

    private Mono<Void> handleLogin(PlayerSession ps, byte[] payload) {
        if (!WORLD_READY) {
            Emitters.sendLoginAck(ps, LOGIN_ERR_SERVER_NOTREADY, 0);
            analyticsHandler.track(ps, "LOGIN_FAILED_SERVER_NOTREADY", "AUTH");
            return Mono.empty();
        }
        if (!LOGIN_LIMITER.tryAcquire()) {
            log.warn("[login] SERVER_BUSY — semaphore đầy (max=128), user={}", ps.getSessionId());
            Emitters.sendLoginAck(ps, LOGIN_ERR_SERVER_BUSY, 0);
            analyticsHandler.track(ps, "LOGIN_FAILED_SERVER_BUSY", "AUTH");
            return Mono.empty();
        }

        final Msglogin.PB_CSLoginToAccount req;
        try {
            req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
        } catch (Exception e) {
            log.warn("[login] parse 7056 error: {}", e.toString());
            Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
            LOGIN_LIMITER.release();
            return Mono.empty();
        }

        final String token = req.hasLoginStr() ? req.getLoginStr() : ps.getSessionId();
        if (token == null || token.isBlank()) {
            log.warn("[login] missing login_str");
            Emitters.sendLoginAck(ps, LOGIN_ERR_MISSING_TOKEN, 0);
            LOGIN_LIMITER.release();
            return Mono.empty();
        }

        final var createdNow   = new java.util.concurrent.atomic.AtomicBoolean(false);
        // ── timing checkpoints ───────────────────────────��──────────────────
        final long t0 = System.currentTimeMillis();
        final AtomicLong tAfterIntrospect  = new AtomicLong();
        final AtomicLong tAfterListByUser  = new AtomicLong();
        final AtomicLong tAfterRoleBind    = new AtomicLong();
        log.info("[login] START token={}... semaphoreAvail={}",
                token.length() > 8 ? token.substring(0, 8) : token,
                LOGIN_LIMITER.availablePermits());
        analyticsHandler.track(ps, "LOGIN_START", "AUTH",
                Map.of("tokenPrefix", token.length() > 8 ? token.substring(0, 8) : token));

        return FeignCall.withToken(token, "session.introspect", () -> sessionFeign.introspect(token))
            .retryWhen(loginRetrySpec("session.introspect"))
                .switchIfEmpty(Mono.error(new IllegalStateException("introspect EMPTY")))
                .flatMap(ir -> {
                    tAfterIntrospect.set(System.currentTimeMillis());
                    log.info("[login] ✔ introspect {}ms", tAfterIntrospect.get() - t0);

                    long forbidRemain = calcForbidRemainSec(ir);
                    if (forbidRemain > 0) {
                        Emitters.sendLoginAck(ps, LOGIN_ERR_FORBID, (int) forbidRemain);
                        analyticsHandler.track(ps, "LOGIN_FAILED_FORBIDDEN", "AUTH",
                                Map.of("forbidRemainSec", forbidRemain));
                        return Mono.empty();
                    }

                    ps.setLoggedIn(true);
                    ps.setSessionId(token);
                    ps.setAnalyticsSessionId(resolveAnalyticsSessionId(ir, token));
                    ps.setUserId(safeStr(invokeStr(ir, "getUserId")));
                    ps.setUsername(safeStr(invokeStr(ir, "getUsername")));

                    if (ps.getUserId() == null || ps.getUserId().isBlank()) {
                        log.warn("[login] introspect ok nhưng userId rỗng");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }

                    log.info("[login] userId={} — calling role.listByUser", ps.getUserId());
                    return kickOldSessionIfAny(ps)
                            .timeout(Duration.ofSeconds(3), Mono.empty())
                            .onErrorResume(e -> Mono.empty())
                            .then(FeignCall.withToken(ps.getSessionId(), "role.listByUser",
                                            () -> roleFeign.listByUser(ps.getUserId()))
                                .retryWhen(loginRetrySpec("role.listByUser"))
                                    .switchIfEmpty(Mono.just(List.of())));
                })
                .flatMap(list -> {
                    tAfterListByUser.set(System.currentTimeMillis());
                    log.info("[login] ✔ role.listByUser {}ms (total {}ms) — roles={}",
                            tAfterListByUser.get() - tAfterIntrospect.get(),
                            tAfterListByUser.get() - t0,
                            list == null ? 0 : list.size());

                    RoleDTOs.RoleResp role = (list != null && !list.isEmpty()) ? pickRole(list) : null;
                    if (role == null) {
                        log.info("[login] userId={} — no role found, calling role.create", ps.getUserId());
                        var createReq = buildCreateRoleReq(ps, req);
                        return FeignCall.withToken(ps.getSessionId(), "role.create",
                                        () -> roleFeign.create(createReq))
                                .doOnNext(newRole -> {
                                    registry.bindRoleToSession(ps, Long.parseLong(newRole.getRoleId()),
                                            newRole.getUserId(), newRole.getName());
                                    createdNow.set(true);
                                    tAfterRoleBind.set(System.currentTimeMillis());
                                    log.info("[login] ✔ role.create {}ms (total {}ms) roleId={}",
                                            tAfterRoleBind.get() - tAfterListByUser.get(),
                                            tAfterRoleBind.get() - t0, newRole.getRoleId());
                                });
                    } else {
                        registry.bindRoleToSession(ps, Long.parseLong(role.getRoleId()),
                                role.getUserId(), role.getName());
                        tAfterRoleBind.set(System.currentTimeMillis());
                        log.info("[login] ✔ role.bind (existing) {}ms (total {}ms) roleId={}",
                                tAfterRoleBind.get() - tAfterListByUser.get(),
                                tAfterRoleBind.get() - t0, role.getRoleId());
                        return Mono.just(role);
                    }
                })
                .flatMap(role -> {
                    if (ps.getRoleId() == null) {
                        log.warn("[login] roleId rỗng sau khi bind");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }
                    LoginSnapshotService.SnapshotAssessment snapshotAssessment =
                        loginSnapshotService.assess(ps.getRoleId());
                    log.info("[login-snapshot] assess roleId={} status={} staleModules={}",
                        ps.getRoleId(), snapshotAssessment.getStatus(), snapshotAssessment.getStaleModules());

                    long tAck = System.currentTimeMillis();
                    log.info("[login] ✔ LOGIN_OK userId={} roleId={} totalLoginMs={}",
                            ps.getUserId(), ps.getRoleId(), tAck - t0);
                    // Track login thành công
                    analyticsHandler.track(ps, "LOGIN_SUCCESS", "AUTH",
                            Map.of("userId",       ps.getUserId(),
                                   "roleId",       String.valueOf(ps.getRoleId()),
                                   "totalLoginMs", tAck - t0,
                               "newRole",      createdNow.get(),
                               "snapshotStatus", snapshotAssessment.getStatus()));

                    // ── Gửi ACK cho client ──────────────────────────────────────────────
                    Emitters.sendLoginAck(ps, LOGIN_OK, 0);
                    Emitters.sendTimeAck(ps, (int) (System.currentTimeMillis() / 1000L), 0);
                    roleServiceHandler.emitRoleInfoAck(ps, role);
                    if (createdNow.get()) {
                        skipFreshRoleDeferredModulesOnce.put(ps.getRoleId(), Boolean.TRUE);
                        log.info("[login] created new role for user {} — skip first deferred non-core gameplay bootstrap", ps.getUserId());
                    }
                    // ── Tải ngay tất cả core game data và user database data ──────────────
                    // Thay vì chờ CS_ALL_INFO_REQ (1450), load toàn bộ data ngay sau login ACK
                    log.info("[login] immediately loading all core game data and user DB data for roleId={}", ps.getRoleId());
                    initializeActivityData(ps);
                    triggerBootstrapOnAllInfo(ps, "LOGIN_IMMEDIATE");
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.warn("[login] flow error after {}ms → ACK PARSE: {}",
                            System.currentTimeMillis() - t0, e.toString());
                    analyticsHandler.track(ps, "LOGIN_FAILED_ERROR", "AUTH",
                            Map.of("error", e.getClass().getSimpleName(),
                                   "msg",   e.getMessage() != null ? e.getMessage() : "",
                                   "elapsedMs", System.currentTimeMillis() - t0));
                    Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                    return Mono.empty();
                })
                .doFinally(sig -> LOGIN_LIMITER.release())
                .then();
    }

    private Retry loginRetrySpec(String phase) {
        return Retry.backoff(3, Duration.ofMillis(120))
                .maxBackoff(Duration.ofSeconds(1))
                .filter(this::isRetryableBootstrapError)
                .doBeforeRetry(sig -> log.warn("[login] {} retry#{} cause={}",
                        phase, sig.totalRetries() + 1, sig.failure().toString()));
    }

    private boolean isRetryableBootstrapError(Throwable t) {
        Throwable e = unwrap(t);
        if (e instanceof java.util.concurrent.TimeoutException) return true;
        if (e instanceof RetryableException) return true;
        if (e instanceof FeignException fe) {
            return fe.status() == -1 || fe.status() >= 500 || fe.status() == 429;
        }
        String msg = e.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("timeout") || lower.contains("connection refused") || lower.contains("connection reset");
    }

    private Throwable unwrap(Throwable t) {
        Throwable cur = t;
        int guard = 0;
        while (cur.getCause() != null && guard++ < 5) {
            cur = cur.getCause();
        }
        return cur;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOOTSTRAP — hoàn toàn độc lập, mỗi service là 1 virtual thread riêng
    // ─────────────────────────────────────────────────────────────────────────

    private void scheduleBootstrapFallback(PlayerSession ps) {
        // Note: Phương thức này không còn được sử dụng vì bootstrap được trigger ngay trong login flow.
        // Giữ lại để tương thích code, nhưng không được gọi nữa.
        Long roleId = ps.getRoleId();
        if (roleId == null) {
            return;
        }

        log.debug("[login] scheduling fallback bootstrap in {}ms roleId={}",
                ALL_INFO_FALLBACK_DELAY_MS, roleId);

        Mono.delay(Duration.ofMillis(ALL_INFO_FALLBACK_DELAY_MS), Schedulers.parallel())
                .subscribe(ignored -> {
                    if (!ps.isLoggedIn() || ps.getRoleId() == null) {
                        log.debug("[login] skip fallback bootstrap because session closed or role missing ws={}",
                                ps.getWs().getId());
                        return;
                    }
                    triggerBootstrapOnAllInfo(ps, "LOGIN_ACK_FALLBACK");
                }, err -> log.debug("[login] fallback bootstrap timer error roleId={}: {}",
                        roleId, err.toString()));
    }

    /**
     * Trigger full bootstrap để load tất cả core game data và user database data.
     * Được gọi ngay sau khi login thành công để load data ngay lập tức.
     *
     * Bootstrap được chia làm 2 wave:
     * - CORE: role/bag/equip/task/box → dữ liệu hiện ngay trên main UI cần được warm sớm
     * - DEFERRED: skill và các module gameplay/phụ trợ khác tải song song
     *
     * Các service có static JSON nặng (task/equip/skill/box) phải ưu tiên đi qua Redis-preload
     * đã warm ở startup; `StartupDependencyReadiness` chỉ mở login khi phần preload này sẵn sàng.
     *
     * Guard BOOTSTRAP_REPLAY_GUARD_MS ngăn chặn việc load trùng lặp nếu client gửi CS_ALL_INFO_REQ.
     */
    private void triggerBootstrapOnAllInfo(PlayerSession ps, String trigger) {
        Long roleId = ps.getRoleId();
        if (roleId == null) {
            log.warn("[bootstrap] ignore trigger={} because roleId is null", trigger);
            return;
        }

        long now = System.currentTimeMillis();
        AtomicLong lastStart = bootstrapLastStartMs.computeIfAbsent(roleId, k -> new AtomicLong(0));
        long previous = lastStart.get();
        if (previous > 0 && (now - previous) < BOOTSTRAP_REPLAY_GUARD_MS) {
            log.info("[bootstrap] skip duplicate trigger={} roleId={} deltaMs={}",
                    trigger, roleId, now - previous);
            return;
        }

        AtomicBoolean inFlight = bootstrapInFlight.computeIfAbsent(roleId, k -> new AtomicBoolean(false));
        if (!inFlight.compareAndSet(false, true)) {
            log.info("[bootstrap] already running trigger={} roleId={}", trigger, roleId);
            return;
        }

        lastStart.set(now);
        pushBootstrap(ps, trigger, inFlight);
    }

    private void pushBootstrap(PlayerSession ps, String trigger, AtomicBoolean inFlight) {
        final long t0Bootstrap = System.currentTimeMillis();
        log.info("[bootstrap] START trigger={} userId={} roleId={} — core-first + deferred",
                trigger, ps.getUserId(), ps.getRoleId());

        buildCoreBootstrap(ps, t0Bootstrap)
                .doOnSuccess(v -> log.info("[bootstrap] CORE DONE trigger={} userId={} roleId={} coreMs={}",
                        trigger, ps.getUserId(), ps.getRoleId(), System.currentTimeMillis() - t0Bootstrap))
                .then(buildDeferredBootstrap(ps, t0Bootstrap))
                .doOnSuccess(v -> {
                    long elapsed = System.currentTimeMillis() - t0Bootstrap;
                    log.info("[bootstrap] ALL DONE trigger={} userId={} roleId={} totalMs={}",
                            trigger, ps.getUserId(), ps.getRoleId(), elapsed);
                    loginSnapshotService.writeBootstrapSnapshot(ps.getRoleId(), elapsed);
                    analyticsHandler.track(ps, "BOOTSTRAP_COMPLETE", "SYSTEM",
                            Map.of("trigger", trigger, "totalMs", elapsed));
                })
                .doFinally(sig -> inFlight.set(false))
                .subscribe(
                        null,
                        e -> {
                            log.warn("[bootstrap] unexpected error trigger={} userId={}: {}",
                                    trigger, ps.getUserId(), e.getMessage());
                            analyticsHandler.track(ps, "BOOTSTRAP_ERROR", "ERROR",
                                    Map.of("trigger", trigger,
                                           "error", e.getClass().getSimpleName(),
                                           "msg",   e.getMessage() != null ? e.getMessage() : ""));
                        }
                );
        log.debug("[bootstrap] subscribed trigger={} userId={} roleId={}",
                trigger, ps.getUserId(), ps.getRoleId());
    }

    private Mono<Void> buildCoreBootstrap(PlayerSession ps, long t0Bootstrap) {
        return Mono.when(
                safe(() -> roleServiceHandler.pushAll(ps), "role", ps, t0Bootstrap),
                safe(() -> bagHandler.pushAll(ps), "bag", ps, t0Bootstrap),
                safe(() -> equipHandler.pushAll(ps), "equip", ps, t0Bootstrap),
                // Main-screen task/tutorial state is needed immediately after entering the game.
                safe(() -> taskHandler.reportDailyLogin(ps).then(taskHandler.pushAll(ps)), "task", ps, t0Bootstrap),
                // The chest widget is also rendered on the main screen, so warm it with the core wave
                // instead of waiting for the later deferred gameplay batch.
                safe(() -> boxHandler.pushAll(ps), "box", ps, t0Bootstrap)
        );
    }

    private Mono<Void> buildDeferredBootstrap(PlayerSession ps, long t0Bootstrap) {
        boolean skipFreshRoleDeferredModules = consumeSkipFreshRoleDeferredModules(ps.getRoleId());
        if (skipFreshRoleDeferredModules) {
            log.info("[bootstrap] skip initial non-core visual/progression bootstrap for freshly created roleId={}", ps.getRoleId());
        }

        // Warm frequently opened gameplay panels in the background right after login.
        // The client still lazy-loads FairyGUI packages locally, and it does not proactively
        // issue CS_FEATURE_DATA_REQ for every first-open path, so excluding these modules made
        // `task`, `box`, `guild`, `friend`, and `activity` feel slow even though login ACK was fast.
        // `box` now runs in the core wave because the main screen renders it immediately.
        return Mono.when(
                safe(() -> skillHandler.pushAll(ps), "skill", ps, t0Bootstrap),
                safe(() -> openServerActivityHandler.pushAll(ps), "activity", ps, t0Bootstrap),
                safe(() -> blockHandler.pushAll(ps), "block", ps, t0Bootstrap),
                safe(() -> waBaoHandler.pushAll(ps), "wabao", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> shiZhuangHandler.pushAll(ps), "shizhuang", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> gemHandler.pushAll(ps), "gem", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> scrollHandler.pushAll(ps), "scroll", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> pagodaHandler.pushAll(ps), "pagoda", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> lingZhuHandler.pushAll(ps), "lingzhu", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> runeHandler.pushAll(ps), "rune", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> shenQiHandler.pushAll(ps), "shenqi", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> petHandler.pushAll(ps), "pet", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> angelHandler.pushAll(ps), "angel", ps, t0Bootstrap),
                maybeSkipFreshRoleDeferredModule(skipFreshRoleDeferredModules, () -> mountHandler.pushAll(ps), "mount", ps, t0Bootstrap),
                safe(() -> friendHandler.pushAll(ps), "friend", ps, t0Bootstrap),
                safe(() -> mailHandler.pushAll(ps), "mail", ps, t0Bootstrap),
                safe(() -> starMapHandler.pushAll(ps), "starmap", ps, t0Bootstrap),
                safe(() -> arenaHandler.pushAll(ps), "arena", ps, t0Bootstrap),
                safe(() -> escortHandler.pushAll(ps), "escort", ps, t0Bootstrap),
                safe(() -> territoryHandler.pushAll(ps), "territory", ps, t0Bootstrap),
                safe(() -> guildHandler.pushAll(ps), "guild", ps, t0Bootstrap),
                safe(() -> mainFbHandler.pushAll(ps), "mainfb", ps, t0Bootstrap)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAFE WRAPPER
    // ─────────────────────────────────────────────────────────────────────────

    private static final Duration PUSH_TIMEOUT = Duration.ofSeconds(12);

    /**
     * Bọc một pushAll() với đầy đủ fault-isolation + timing log chi tiết.
     * <ul>
     *   <li><b>START</b>  — log ngay khi service bắt đầu chạy (trên virtual thread)</li>
     *   <li><b>DONE</b>   — log khi service trả về, kèm elapsed ms</li>
     *   <li><b>SLOW</b>   — WARN nếu >{@code SLOW_WARN_MS}ms, ERROR nếu >{@code SLOW_ERROR_MS}ms</li>
     *   <li><b>TIMEOUT</b>— log khi service treo quá {@code PUSH_TIMEOUT}</li>
     *   <li><b>ERROR</b>  — log lỗi cụ thể (exception class + message)</li>
     * </ul>
     */
    private Mono<Void> safe(Supplier<Mono<Void>> supplier, String name,
                            PlayerSession ps, long t0Bootstrap) {
        final AtomicLong tStart = new AtomicLong();
        return Mono.defer(() -> {
                    tStart.set(System.currentTimeMillis());
                    // ── CỐT LÕI FIX: set token vào ThreadLocal CỦA virtual thread này ──────
                    // FeignAuthInterceptor đọc FeignTokenHolder.get() để thêm Authorization header.
                    // Các handler dùng Mono.fromRunnable() không tự set token → phải set ở đây,
                    // TRƯỚC khi supplier.get() tạo và subscribe Mono của handler.
                    FeignTokenHolder.set(ps.getSessionId());
                    log.info("[bootstrap] {} START  userId={} (+{}ms)",
                            name, ps.getUserId(), tStart.get() - t0Bootstrap);
                    return supplier.get();
                })
                .subscribeOn(feignScheduler)
                .timeout(PUSH_TIMEOUT)
                .doOnSuccess(v -> {
                    long elapsed = System.currentTimeMillis() - tStart.get();
                    if (elapsed > SLOW_ERROR_MS) {
                        log.error("[bootstrap] {} SLOW-ERROR  {}ms (>{})  userId={}",
                                name, elapsed, SLOW_ERROR_MS, ps.getUserId());
                    } else if (elapsed > SLOW_WARN_MS) {
                        log.warn("[bootstrap] {} SLOW-WARN   {}ms (>{})  userId={}",
                                name, elapsed, SLOW_WARN_MS, ps.getUserId());
                    } else {
                        log.info("[bootstrap] {} DONE        {}ms  userId={}",
                                name, elapsed, ps.getUserId());
                    }
                })
                .doFinally(sig -> FeignTokenHolder.clear())   // ← dọn ThreadLocal sau khi xong
                .onErrorResume(e -> {
                    long elapsed = tStart.get() > 0 ? System.currentTimeMillis() - tStart.get() : -1;
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("[bootstrap] {} TIMEOUT     >{}s elapsed={}ms  userId={} — service TREO!",
                                name, PUSH_TIMEOUT.toSeconds(), elapsed, ps.getUserId());
                    } else {
                        log.warn("[bootstrap] {} ERROR        {}ms  userId={}  cause={}: {}",
                                name, elapsed, ps.getUserId(),
                                e.getClass().getSimpleName(), e.getMessage());
                    }
                    return Mono.empty();
                });
    }

    /** Overload cũ giữ tương thích nếu còn dùng ��� nơi khác */
    @SuppressWarnings("unused")
    private Mono<Void> safe(Supplier<Mono<Void>> supplier, String handlerName) {
        // Tạo dummy session để tương thích; caller nên dùng overload 4 tham số
        return Mono.defer(supplier)
                .subscribeOn(feignScheduler)
                .timeout(PUSH_TIMEOUT)
                .onErrorResume(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("[bootstrap] {} TIMEOUT >{}s — service TREO!",
                                handlerName, PUSH_TIMEOUT.toSeconds());
                    } else {
                        log.warn("[bootstrap] {} ERROR cause={}: {}",
                                handlerName, e.getClass().getSimpleName(), e.getMessage());
                    }
                    return Mono.empty();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private RoleDTOs.RoleResp pickRole(List<RoleDTOs.RoleResp> list) {
        return list.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(RoleDTOs.RoleResp::getLevel, Comparator.nullsFirst(Integer::compareTo)))
                .orElse(list.getFirst());
    }

    private RoleDTOs.CreateRoleReq buildCreateRoleReq(PlayerSession ps, Msglogin.PB_CSLoginToAccount req) {
        var r = new RoleDTOs.CreateRoleReq();
        tryInvokeSetter(r, "setUserId", ps.getUserId());
        String name = (req.hasPname() && !req.getPname().isBlank())
                ? req.getPname() : defaultRoleName(ps.getUserId());
        tryInvokeSetter(r, "setName", name);
        tryInvokeSetter(r, "setNickname", name);
        tryInvokeSetter(r, "setRoleName", name);
        int serverId = req.hasServer() ? req.getServer() : 1;
        tryInvokeSetter(r, "setServerId", serverId);
        return r;
    }

    private String defaultRoleName(String userId) {
        String suffix = (userId != null && userId.length() >= 4)
                ? userId.substring(userId.length() - 4) : "0000";
        return "Player_" + suffix;
    }

    private Mono<Void> kickOldSessionIfAny(PlayerSession newPs) {
        var others = registry.sessionsOfUser(newPs.getUserId()).stream()
                .filter(s -> !s.getWs().getId().equals(newPs.getWs().getId()))
                .toList();
        if (others.isEmpty()) return Mono.empty();
        for (var o : others) {
            try { Emitters.sendDisconnectNotice(o, DISCONNECT_REASON_LOGIN_OTHER_PLACE); } catch (Throwable ignore) {}
            try { o.getWs().close().subscribe(); } catch (Throwable ignore) {}
        }
        return Mono.empty();
    }

    private long calcForbidRemainSec(Object ir) {
        long now = System.currentTimeMillis() / 1000L;
        Long until = invokeLong(ir, "getForbidUntilEpochSec");
        if (until == null) until = invokeLong(ir, "getForbidUntil");
        if (until == null) until = invokeLong(ir, "getForbidTime");
        if (until == null || until <= 0) return 0;
        return Math.max(0, until - now);
    }

    private void tryInvokeSetter(Object bean, String setter, Object val) {
        try {
            Method m = null;
            try { m = bean.getClass().getMethod(setter, val.getClass()); } catch (NoSuchMethodException ignore) {}
            if (m == null && val instanceof Integer) m = bean.getClass().getMethod(setter, int.class);
            if (m == null && val instanceof Long)    m = bean.getClass().getMethod(setter, long.class);
            if (m == null && val instanceof Boolean) m = bean.getClass().getMethod(setter, boolean.class);
            if (m == null && val instanceof String)  m = bean.getClass().getMethod(setter, String.class);
            if (m != null) m.invoke(bean, val);
        } catch (Exception ignore) {}
    }

    private Long invokeLong(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            if (v == null) return null;
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) { return null; }
    }

    private String invokeStr(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) { return null; }
    }

    private String resolveAnalyticsSessionId(Object introspectResp, String token) {
        String introspectedSessionId = invokeStr(introspectResp, "getSessionId");
        if (introspectedSessionId != null && !introspectedSessionId.isBlank()) {
            return introspectedSessionId;
        }
        return extractSidFromJwt(token);
    }

    private String extractSidFromJwt(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || parts[1].isBlank()) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = JSON.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode sidNode = claims.get("sid");
            if (sidNode == null || sidNode.isNull()) {
                return null;
            }
            String sid = sidNode.asText(null);
            return sid == null || sid.isBlank() ? null : sid.trim();
        } catch (Exception e) {
            log.debug("[login] failed to extract sid from token: {}", e.getMessage());
            return null;
        }
    }

    private String safeStr(String s) {
        return (s == null || s.isBlank()) ? "" : s;
    }

    private Mono<Void> maybeSkipFreshRoleDeferredModule(boolean skipFreshRoleDeferredModules,
                                                        Supplier<Mono<Void>> supplier,
                                                        String name,
                                                        PlayerSession ps,
                                                        long t0Bootstrap) {
        if (!skipFreshRoleDeferredModules) {
            return safe(supplier, name, ps, t0Bootstrap);
        }
        log.debug("[bootstrap] {} skipped on first deferred wave for freshly created roleId={}",
                name, ps.getRoleId());
        return Mono.empty();
    }

    private boolean consumeSkipFreshRoleDeferredModules(Long roleId) {
        return roleId != null && skipFreshRoleDeferredModulesOnce.remove(roleId) != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIVITY SERVICE INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialize activity-service data on login.
     * This ensures activity states (SevenDaySign, etc.) are created/initialized
     * and login events are tracked in the activity system.
     * Fire-and-forget to avoid blocking the login flow.
     */
    private void initializeActivityData(PlayerSession ps) {
        if (ps.getRoleId() == null) {
            log.warn("[activity-init] roleId is null, skipping activity initialization");
            return;
        }

        final String roleIdStr = String.valueOf(ps.getRoleId());
        final String token = ps.getSessionId();

        // Fire-and-forget initialization - don't block login flow
        Mono.fromRunnable(() -> {
            try {
                FeignTokenHolder.set(token);

                // Initialize SevenDaySign activity (auto-creates if not exists)
                try {
                    var sevenDayData = activityFeign.getSevenDay(roleIdStr);
                    log.info("[activity-init] SevenDaySign initialized for roleId={}", ps.getRoleId());

                    // Track activity login event
                    analyticsHandler.track(ps, "ACTIVITY_LOGIN", "GAMEPLAY",
                            Map.of("roleId", roleIdStr,
                                   "activityType", "SevenDaySign",
                                   "sevenDayData", sevenDayData != null ? sevenDayData.toString() : "null"));
                } catch (Exception e) {
                    log.warn("[activity-init] Failed to initialize SevenDaySign for roleId={}: {}",
                            ps.getRoleId(), e.getMessage());
                }

                // Initialize other key activities that should be auto-created on login
                try {
                    activityFeign.getLuck(roleIdStr);
                    log.debug("[activity-init] LuckUnpacking initialized for roleId={}", ps.getRoleId());
                } catch (Exception e) {
                    log.warn("[activity-init] Failed to initialize LuckUnpacking for roleId={}: {}",
                            ps.getRoleId(), e.getMessage());
                }

                try {
                    activityFeign.getNewArea(roleIdStr);
                    log.debug("[activity-init] NewAreaPreferential initialized for roleId={}", ps.getRoleId());
                } catch (Exception e) {
                    log.warn("[activity-init] Failed to initialize NewAreaPreferential for roleId={}: {}",
                            ps.getRoleId(), e.getMessage());
                }

                log.info("[activity-init] Activity initialization completed for roleId={}", ps.getRoleId());
            } catch (Exception e) {
                log.warn("[activity-init] Unexpected error during activity initialization for roleId={}: {}",
                        ps.getRoleId(), e.getMessage());
            } finally {
                FeignTokenHolder.clear();
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(
                null,
                e -> log.warn("[activity-init] Activity initialization failed for roleId={}: {}",
                        ps.getRoleId(), e.getMessage())
        );
    }
}
