package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
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
public class GameWorldGrpcClient {

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
     * Pickup item from world
     * @return PickupItemResponse with item details or error code
     */
    public PickupItemResponse pickupItem(Long roleId, long itemUid, int zoneId, float playerX, float playerY, float playerZ) {
        try {
            Position playerPosition = Position.newBuilder()
                    .setX(playerX)
                    .setY(playerY)
                    .setZ(playerZ)
                    .build();

            PickupItemRequest request = PickupItemRequest.newBuilder()
                    .setRoleId(roleId)
                    .setItemUid(itemUid)
                    .setZoneId(zoneId)
                    .setPlayerPosition(playerPosition)
                    .build();

            PickupItemResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .pickupItem(request);

            if (response.getSuccess()) {
                log.info("Player {} picked up item {}: itemId={}, quantity={}",
                        roleId, itemUid, response.getItemId(), response.getQuantity());
            } else {
                log.warn("Player {} failed to pickup item {}: errorCode={}",
                        roleId, itemUid, response.getErrorCode());
            }

            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to pickup item {} for role: {}", itemUid, roleId, e);
            return PickupItemResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorCode("GRPC_ERROR")
                    .setErrorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Interact with NPC
     * @return InteractNpcResponse with interaction result
     */
    public InteractNpcResponse interactNpc(Long roleId, int npcId, int interactType, int zoneId) {
        try {
            InteractNpcRequest request = InteractNpcRequest.newBuilder()
                    .setRoleId(roleId)
                    .setNpcId(npcId)
                    .setInteractType(interactType)
                    .setZoneId(zoneId)
                    .build();

            InteractNpcResponse response = gameWorldServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .interactNpc(request);

            if (response.getSuccess()) {
                log.info("Player {} interacted with NPC {}: type={}, npcType={}",
                        roleId, npcId, interactType, response.getNpcType());
            } else {
                log.warn("Player {} failed to interact with NPC {}: errorCode={}",
                        roleId, npcId, response.getErrorCode());
            }

            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to interact with NPC {} for role: {}", npcId, roleId, e);
            return InteractNpcResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorCode("GRPC_ERROR")
                    .setErrorMessage(e.getMessage())
                    .build();
        }
    }
}
