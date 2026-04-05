package com.SouthMillion.drop_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports whether all required drop XML tables are already warm in Redis.
 * When strict Redis-first is enabled, this health check must be UP before login should open.
 */
@Slf4j
@Component("dropRedis")
@RequiredArgsConstructor
public class DropRedisHealthIndicator implements HealthIndicator {

    private final DropRedisStatusService statusService;

    @Override
    public Health health() {
        DropRedisStatusService.DropRedisStatus status = statusService.snapshot();

        if (!status.redisEnabled() || status.knownDropCount() == 0) {
            return Health.unknown()
                    .withDetail("redisEnabled", status.redisEnabled())
                    .withDetail("strictRedisFirst", status.strictRedisFirst())
                    .withDetail("knownDropCount", status.knownDropCount())
                    .withDetail("reason", status.reason())
                    .build();
        }

        Health.Builder builder = status.ready() ? Health.up() : Health.down();
        builder.withDetail("strictRedisFirst", status.strictRedisFirst())
                .withDetail("knownDropCount", status.knownDropCount())
                .withDetail("cachedCount", status.cachedCount())
                .withDetail("missingCount", status.missingCount())
                .withDetail("reason", status.reason());

        if (status.firstMissingId() != null) {
            builder.withDetail("missingSample", status.missingRanges())
                    .withDetail("firstMissingId", status.firstMissingId());
        }

        return builder.build();
    }
}
