package com.SouthMillion.webSocket_server.handler;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import reactor.core.publisher.Mono;

/**
 * Interface for handlers that support lazy loading.
 *
 * <p>Lazy-loaded handlers are NOT invoked during initial login bootstrap.
 * Instead, they load data on-demand when the player opens the corresponding UI panel.
 * This significantly reduces initial login time by deferring non-essential data.
 *
 * <p>The client sends CS_FEATURE_DATA_REQ (msgId 1453) with a module name when opening
 * a specific UI panel. The server looks up the appropriate LazyLoadHandler and invokes
 * its {@link #loadOnDemand(PlayerSession)} method.
 *
 * <p><b>Example modules:</b>
 * <ul>
 *   <li>"activity" → OpenServerActivityHandler (七日签到, 开箱大吉, etc.)</li>
 *   <li>"friend" → FriendHandler (friends list + blacklist)</li>
 *   <li>"guild" → GuildHandler (guild info + members)</li>
 *   <li>"box" → BoxHandler (treasure chest state)</li>
 *   <li>"escort" → EscortHandler (escort mission)</li>
 *   <li>"territory" → TerritoryHandler (territory conquest)</li>
 * </ul>
 *
 * @see com.SouthMillion.webSocket_server.handler.LazyDataRequestHandler
 */
public interface LazyLoadHandler {

    /**
     * Returns the module name for this handler.
     * The client uses this name in CS_FEATURE_DATA_REQ to identify which module to load.
     *
     * @return the module name (e.g., "activity", "friend", "guild")
     */
    String getModuleName();

    /**
     * Load data on demand when the player opens the corresponding UI panel.
     * This method is called by {@link LazyDataRequestHandler} when receiving CS_FEATURE_DATA_REQ.
     *
     * <p>Implementation should:
     * <ul>
     *   <li>Fetch data from backend services via Feign/gRPC</li>
     *   <li>Emit appropriate protocol messages to the client</li>
     *   <li>Handle errors gracefully (log + emit empty/fallback data)</li>
     *   <li>Return quickly (use timeouts, avoid blocking)</li>
     * </ul>
     *
     * @param ps the player session
     * @return a Mono that completes when data is loaded and emitted
     */
    Mono<Void> loadOnDemand(PlayerSession ps);
}
