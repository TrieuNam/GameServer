package com.SouthMillion.webSocket_server.handler.crafting;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.CraftingGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.grpc.crafting.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Crafting Handler - Handles crafting/forging operations via gRPC
 * Messages: 1700-1709 (craft operations)
 * 
 * Operations:
 * - reqType 1: Get available recipes
 * - reqType 2: Start crafting
 * - reqType 3: Get crafting status
 * - reqType 4: Claim completed items
 * 
 * Integration: Uses CraftingGrpcClient for high-performance gRPC communication
 * Protocol: Simplified binary format for WebSocket transmission
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CraftingHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CraftingHandler.class);

    private final CraftingGrpcClient craftingGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    @Override
    public int[] interests() {
        return new int[]{
            MessageIds.CS_CRAFT_REQ
        };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        if (ps.getRoleId() == null) {
            log.warn("[crafting] User not logged in: sessionId={}", ps.getSessionId());
            return;
        }

        log.info("[crafting] Handle msgId={} for roleId={}", msgId, ps.getRoleId());

        try {
            if (msgId == MessageIds.CS_CRAFT_REQ) {
                handleCraftReq(ps, payload);
            }
        } catch (Exception e) {
            log.error("[crafting] Error handling msgId={}", msgId, e);
        }
        });
    }

    private void handleCraftReq(PlayerSession ps, byte[] payload) {
        try {
            // Parse simple binary format: reqType(4) + param1(4) + param2(4)
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            int reqType = payload.length >= 4 ? buffer.getInt() : 0;
            int param1 = payload.length >= 8 ? buffer.getInt() : 0;
            int param2 = payload.length >= 12 ? buffer.getInt() : 0;

            log.info("[crafting] Craft request: roleId={}, reqType={}, param1={}, param2={}",
                     ps.getRoleId(), reqType, param1, param2);

            switch (reqType) {
                case 1: // Get recipes
                    handleGetRecipes(ps, param1);
                    break;
                case 2: // Start crafting
                    handleStartCraft(ps, param1, param2);
                    break;
                case 3: // Get status
                    handleGetStatus(ps);
                    break;
                case 4: // Claim completed
                    handleClaim(ps, param1);
                    break;
                default:
                    log.warn("[crafting] Unknown reqType: {}", reqType);
            }

        } catch (Exception e) {
            log.error("[crafting] Failed to process craft request", e);
        }
    }

    private void handleGetRecipes(PlayerSession ps, int level) {
        try {
            // Get available recipes via gRPC
            List<Recipe> recipes = craftingGrpcClient.getRecipes(ps.getRoleId(), level);

            // Build simple binary response: count(4) + [recipeId(4) + itemId(4) + amount(4)]*count
            ByteBuffer buffer = ByteBuffer.allocate(4 + recipes.size() * 12);
            buffer.putInt(recipes.size());
            
            for (Recipe recipe : recipes) {
                buffer.putInt(recipe.getRecipeId());
                buffer.putInt(recipe.getResultItemId());
                buffer.putInt(recipe.getResultAmount());
            }

            // Send response
            Emitters.emit(ps, MessageIds.SC_CRAFT_INFO, buffer.array());

            log.info("[crafting] Sent {} recipes to roleId={}", recipes.size(), ps.getRoleId());

        } catch (Exception e) {
            log.error("[crafting] Failed to get recipes", e);
        }
    }

    private void handleStartCraft(PlayerSession ps, int recipeId, int count) {
        try {
            // Start crafting via gRPC
            StartCraftResponse response = craftingGrpcClient.startCraft(
                ps.getRoleId(), 
                recipeId, 
                count > 0 ? count : 1
            );

            // Build binary response: success(1) + craftingId(8) + endTime(8)
            ByteBuffer buffer = ByteBuffer.allocate(17);
            buffer.put((byte) (response.getSuccess() ? 1 : 0));
            buffer.putLong(response.getCraftingId());
            buffer.putLong(response.getEndTime());

            // Send response
            Emitters.emit(ps, MessageIds.SC_CRAFT_START, buffer.array());
            if (response.getSuccess()) {
                publishTaskProgress(ps.getRoleId(), taskActionConditionMapping.craftingStartTaskKey(), "websocket-crafting-start");
            }

            log.info("[crafting] Started craft: roleId={}, recipeId={}, success={}", 
                    ps.getRoleId(), recipeId, response.getSuccess());

        } catch (Exception e) {
            log.error("[crafting] Failed to start craft", e);
        }
    }

    private void handleGetStatus(PlayerSession ps) {
        try {
            // Get crafting status via gRPC
            List<CraftingStatus> statusList = craftingGrpcClient.getCraftingStatus(ps.getRoleId());

            // Build binary response: count(4) + [craftingId(8) + recipeId(4) + startTime(8) + endTime(8) + canClaim(1)]*count
            ByteBuffer buffer = ByteBuffer.allocate(4 + statusList.size() * 29);
            buffer.putInt(statusList.size());
            
            for (CraftingStatus status : statusList) {
                buffer.putLong(status.getCraftingId());
                buffer.putInt(status.getRecipeId());
                buffer.putLong(status.getStartTime());
                buffer.putLong(status.getEndTime());
                buffer.put((byte) (status.getCanClaim() ? 1 : 0));
            }

            // Send response
            Emitters.emit(ps, MessageIds.SC_CRAFT_STATUS, buffer.array());

            log.info("[crafting] Sent {} crafting status to roleId={}", statusList.size(), ps.getRoleId());

        } catch (Exception e) {
            log.error("[crafting] Failed to get status", e);
        }
    }

    private void handleClaim(PlayerSession ps, int craftingId) {
        try {
            // Claim completed crafting via gRPC
            ClaimCraftingResponse response = craftingGrpcClient.claimCrafting(
                ps.getRoleId(), 
                (long) craftingId
            );

            // Build binary response: success(1) + itemCount(4) + [itemId(4) + amount(4)]*itemCount
            List<RewardItem> items = response.getRewardsList();
            int itemCount = items != null ? items.size() : 0;
            
            ByteBuffer buffer = ByteBuffer.allocate(5 + itemCount * 8);
            buffer.put((byte) (response.getSuccess() ? 1 : 0));
            buffer.putInt(itemCount);
            
            if (items != null) {
                for (RewardItem item : items) {
                    buffer.putInt(item.getItemId());
                    buffer.putInt(item.getAmount());
                }
            }

            // Send response
            Emitters.emit(ps, MessageIds.SC_CRAFT_CLAIM, buffer.array());
            if (response.getSuccess()) {
                publishTaskProgress(ps.getRoleId(), taskActionConditionMapping.craftingClaimTaskKey(), "websocket-crafting-claim");
            }

            log.info("[crafting] Claimed crafting: roleId={}, craftingId={}, success={}", 
                    ps.getRoleId(), craftingId, response.getSuccess());

        } catch (Exception e) {
            log.error("[crafting] Failed to claim", e);
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}
