package com.SouthMillion.webSocket_server.service.grpc;

import com.SouthMillion.webSocket_server.service.client.WorldFeign;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.gameworld.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for Game World Service
 * Replaces GameWorldFeign REST calls with gRPC for 50-60% performance improvement
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameWorldGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameWorldGrpcClient.class);

    private final WorldFeign worldFeign;

    @GrpcClient("gameworld-service")
    private GameWorldServiceGrpc.GameWorldServiceBlockingStub gameWorldServiceStub;

    /**
     * Update player position
     */
    public UpdatePositionResponse updatePosition(Long roleId, int zoneId, float x, float y, float z, 
                                                  float rotation, String movementState) {
        try {
            Position position = Position.newBuilder()
                    .setX(x)
                    .setY(y)
                    .setZ(z)
                    .setRotation(rotation)
                    .build();

            UpdatePositionRequest request = UpdatePositionRequest.newBuilder()
                    .setRoleId(roleId)
                    .setZoneId(zoneId)
                    .setPosition(position)
                    .setMovementState(movementState)
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            UpdatePositionResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .updatePosition(request);
            log.debug("Updated position for role: {} in zone: {}", roleId, zoneId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to update position for role: {}", roleId, e);
            return UpdatePositionResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Get nearby players
     */
    public NearbyPlayersResponse getNearbyPlayers(Long roleId, int zoneId, float centerX, float centerY, 
                                                   float centerZ, float radius, int maxPlayers) {
        try {
            Position centerPosition = Position.newBuilder()
                    .setX(centerX)
                    .setY(centerY)
                    .setZ(centerZ)
                    .build();

            GetNearbyPlayersRequest request = GetNearbyPlayersRequest.newBuilder()
                    .setRoleId(roleId)
                    .setZoneId(zoneId)
                    .setCenterPosition(centerPosition)
                    .setRadius(radius)
                    .setMaxPlayers(maxPlayers)
                    .build();

            NearbyPlayersResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getNearbyPlayers(request);
            log.debug("Got {} nearby players for role: {}", response.getTotalCount(), roleId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get nearby players for role: {}", roleId, e);
            return NearbyPlayersResponse.newBuilder()
                    .setTotalCount(0)
                    .build();
        }
    }

    /**
     * Enter zone/map
     */
    public ZoneInfoResponse enterZone(Long roleId, int zoneId, float spawnX, float spawnY, float spawnZ) {
        try {
            Position spawnPosition = Position.newBuilder()
                    .setX(spawnX)
                    .setY(spawnY)
                    .setZ(spawnZ)
                    .build();

            EnterZoneRequest request = EnterZoneRequest.newBuilder()
                    .setRoleId(roleId)
                    .setZoneId(zoneId)
                    .setSpawnPosition(spawnPosition)
                    .build();

            ZoneInfoResponse response = gameWorldServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .enterZone(request);
            log.info("Role {} entered zone: {}", roleId, zoneId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to enter zone {} for role: {}", zoneId, roleId, e);
            return ZoneInfoResponse.newBuilder().build();
        }
    }

    /**
     * Leave zone/map
     */
    public boolean leaveZone(Long roleId, int zoneId) {
        try {
            LeaveZoneRequest request = LeaveZoneRequest.newBuilder()
                    .setRoleId(roleId)
                    .setZoneId(zoneId)
                    .build();

            org.SouthMillion.grpc.common.ResponseStatus response = gameWorldServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .leaveZone(request);
            log.info("Role {} left zone: {}", roleId, zoneId);
            return response.getCode() == 0;
        } catch (StatusRuntimeException e) {
            log.error("Failed to leave zone {} for role: {}", zoneId, roleId, e);
            return false;
        }
    }

    /**
     * Get zone info
     */
    public ZoneInfoResponse getZoneInfo(int zoneId, boolean includePlayers, boolean includeEntities) {
        try {
            GetZoneInfoRequest request = GetZoneInfoRequest.newBuilder()
                    .setZoneId(zoneId)
                    .setIncludePlayers(includePlayers)
                    .setIncludeEntities(includeEntities)
                    .build();

            ZoneInfoResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getZoneInfo(request);
            log.debug("Got zone info for zone: {}", zoneId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get zone info for zone: {}", zoneId, e);
            return ZoneInfoResponse.newBuilder().build();
        }
    }

    /**
     * Spawn entity (monster, NPC, etc.)
     */
    public EntityResponse spawnEntity(int zoneId, String entityType, int entityId, 
                                      float x, float y, float z) {
        try {
            Position spawnPosition = Position.newBuilder()
                    .setX(x)
                    .setY(y)
                    .setZ(z)
                    .build();

            SpawnEntityRequest request = SpawnEntityRequest.newBuilder()
                    .setZoneId(zoneId)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setSpawnPosition(spawnPosition)
                    .build();

            EntityResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .spawnEntity(request);
            log.debug("Spawned entity {} in zone: {}", entityId, zoneId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to spawn entity {} in zone: {}", entityId, zoneId, e);
            return EntityResponse.newBuilder().build();
        }
    }

    /**
     * Despawn entity
     */
    public boolean despawnEntity(int zoneId, String instanceId, String reason) {
        try {
            DespawnEntityRequest request = DespawnEntityRequest.newBuilder()
                    .setInstanceId(instanceId)
                    .setZoneId(zoneId)
                    .setReason(reason)
                    .build();

            org.SouthMillion.grpc.common.ResponseStatus response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .despawnEntity(request);
            log.debug("Despawned entity {} from zone: {}", instanceId, zoneId);
            return response.getCode() == 0;
        } catch (StatusRuntimeException e) {
            log.error("Failed to despawn entity {} from zone: {}", instanceId, zoneId, e);
            return false;
        }
    }

    /**
     * Pickup item from world.
     * Temporary REST fallback because `gameworld_service.proto` does not define pickup RPCs yet.
     */
    public PickupItemResponse pickupItem(Long roleId, long itemUid, int zoneId, float playerX, float playerY, float playerZ) {
        try {
            String rawResult = worldFeign.pickupItem(roleId, itemUid);

            if (rawResult == null || rawResult.isBlank()) {
                log.warn("Empty pickup response for role {} item {} in zone {}", roleId, itemUid, zoneId);
                return PickupItemResponse.builder()
                        .success(false)
                        .errorCode("EMPTY_RESPONSE")
                        .errorMessage("World service returned empty pickup response")
                        .build();
            }

            String result = rawResult.trim();
            if (result.startsWith("ERROR:")) {
                String message = result.substring("ERROR:".length()).trim();
                log.warn("Pickup failed for role {} item {} in zone {}: {}", roleId, itemUid, zoneId, message);
                return PickupItemResponse.builder()
                        .success(false)
                        .errorCode("WORLD_SERVICE_ERROR")
                        .errorMessage(message.isEmpty() ? "Pickup failed" : message)
                        .build();
            }

            String[] parts = result.split(":", 2);
            if (parts.length != 2) {
                log.warn("Unexpected pickup response format for role {} item {}: {}", roleId, itemUid, result);
                return PickupItemResponse.builder()
                        .success(false)
                        .errorCode("PARSE_ERROR")
                        .errorMessage("Unexpected pickup response: " + result)
                        .build();
            }

            int itemId = Integer.parseInt(parts[0].trim());
            int quantity = Integer.parseInt(parts[1].trim());

            log.info("Player {} picked up item {} via world-service fallback: itemId={}, quantity={}, zoneId={}, pos=({}, {}, {})",
                    roleId, itemUid, itemId, quantity, zoneId, playerX, playerY, playerZ);

            return PickupItemResponse.builder()
                    .success(true)
                    .itemId(itemId)
                    .quantity(quantity)
                    .isBagGranted(true)
                    .build();
        } catch (NumberFormatException e) {
            log.error("Failed to parse pickup response for role {} item {}", roleId, itemUid, e);
            return PickupItemResponse.builder()
                    .success(false)
                    .errorCode("PARSE_ERROR")
                    .errorMessage("Invalid pickup response format")
                    .build();
        } catch (Exception e) {
            log.error("Failed to pickup item {} for role: {}", itemUid, roleId, e);
            return PickupItemResponse.builder()
                    .success(false)
                    .errorCode("WORLD_SERVICE_EXCEPTION")
                    .errorMessage(e.getMessage() == null ? "World service call failed" : e.getMessage())
                    .build();
        }
    }

    /**
     * Interact with NPC.
     * Temporary REST fallback because `gameworld_service.proto` does not define NPC interaction RPCs yet.
     */
    public InteractNpcResponse interactNpc(Long roleId, int npcId, int interactType, int zoneId) {
        try {
            Boolean success = worldFeign.interactNpc(roleId, npcId, interactType);

            if (Boolean.TRUE.equals(success)) {
                String npcType = resolveNpcType(interactType);
                log.info("Player {} interacted with NPC {} via world-service fallback: type={}, npcType={}, zoneId={}",
                        roleId, npcId, interactType, npcType, zoneId);
                return InteractNpcResponse.builder()
                        .success(true)
                        .npcType(npcType)
                        .build();
            }

            log.warn("Player {} failed to interact with NPC {} in zone {}", roleId, npcId, zoneId);
            return InteractNpcResponse.builder()
                    .success(false)
                    .errorCode("WORLD_SERVICE_ERROR")
                    .errorMessage("NPC interaction failed")
                    .build();
        } catch (Exception e) {
            log.error("Failed to interact with NPC {} for role: {}", npcId, roleId, e);
            return InteractNpcResponse.builder()
                    .success(false)
                    .errorCode("WORLD_SERVICE_EXCEPTION")
                    .errorMessage(e.getMessage() == null ? "World service call failed" : e.getMessage())
                    .build();
        }
    }

    private String resolveNpcType(int interactType) {
        return switch (interactType) {
            case 1 -> "DIALOGUE";
            case 2 -> "SHOP";
            case 3 -> "QUEST";
            default -> "UNKNOWN";
        };
    }
}
