package com.SouthMillion.activity_service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.Optional;

/**
 * Feign client for role-service.
 *
 * <p>
 * All methods are protected via Resilience4j circuit breaker, with fallback methods that log failures 
 * and return safe defaults (empty Optionals/nulls as appropriate).
 * <br>
 * <b>Timeout, circuit breaker, and retry settings are externalized via application properties/yaml only!</b>
 * <br>
 * By default, timeout is 2s, max retry attempts is 3, breaker config is "roleServiceCB" (see application config).
 * <br>
 * See sibling clients for the same robustness pattern.
 * <br>
 * <b>Deprecation warning:</b> This synchronous/blocking client may be replaced by WebClient-based non-blocking alternatives.
 * </p>
 *
 * <p>
 * Fallback will return an empty Optional if calls fail. TODO: Reference cache if available.
 * </p>
 *
 * @author your-team
 */
@FeignClient(name = "role-service", path = "/api/role", contextId = "activityRoleFeign")
public interface RoleFeign {

    Logger log = LoggerFactory.getLogger(RoleFeign.class);

    /**
     * Get role details by roleId.
     *
     * <p>
     * Protected by circuit breaker "roleServiceCB". Fallback returns Optional.empty() on error.
     * Timeouts/circuit-breaker/retry are <b>externalized via config</b>.
     * </p>
     *
     * @param roleId Role id to lookup
     * @return Optional RoleResp or empty Optional if not found or remote error
     */
    @CircuitBreaker(name = "roleServiceCB", fallbackMethod = "fallbackDetail")
    @GetMapping("/{roleId}")
    Optional<RoleDTOs.RoleResp> detail(@PathVariable("roleId") Long roleId);

    /**
     * Fallback method for detail. Logs roleId and reason, returns Optional.empty().
     * TODO: If caching introduced, consider serving stale data from cache here.
     */
    default Optional<RoleDTOs.RoleResp> fallbackDetail(Long roleId, Throwable t) {
        log.error("[RoleFeign] detail fallback for roleId={}, cause={}", roleId, t.toString(), t);
        // Standard: always return empty so caller can handle error.
        return Optional.empty();
    }
}