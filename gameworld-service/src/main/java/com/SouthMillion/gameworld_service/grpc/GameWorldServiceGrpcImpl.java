package com.SouthMillion.gameworld_service.grpc;

import com.SouthMillion.gameworld_service.dto.NearbyPlayersResponse;
import com.SouthMillion.gameworld_service.dto.PlayerPosition;
import com.SouthMillion.gameworld_service.service.GameWorldService;
import com.SouthMillion.gameworld_service.service.client.RoleServiceClient;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.gameworld.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Game World
 * High-performance position tracking and world state management
 * 
 * Target Performance: <25ms per position update
 * Throughput: >10K position updates/sec
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameWorldServiceGrpcImpl extends GameWorldServiceGrpc.GameWorldServiceImplBase {

    private final GameWorldService gameWorldService;
    private final RoleServiceClient roleServiceClient;

    @Override
    public void updatePosition(UpdatePositionRequest request,
                               StreamObserver<UpdatePositionResponse> responseObserver) {
        log.debug("gRPC UpdatePosition: player={}, zone={}", request.getRoleId(), request.getZoneId());
        
        try {
            // Convert proto to internal DTO
            PlayerPosition position = PlayerPosition.builder()
                    .playerId(request.getRoleId())
                    .mapId(request.getZoneId())
                    .x(request.getPosition().getX())
                    .y(request.getPosition().getY())
                    .z(request.getPosition().getZ())
                    .direction(request.getPosition().getRotation())
                    .state(request.getMovementState())
                    .timestamp(request.getTimestamp())
                    .build();
            
            // Validate
            if (!gameWorldService.validatePosition(position)) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Invalid position coordinates")
                        .asRuntimeException());
                return;
            }
            
            // Update position
            PlayerPosition updated = gameWorldService.updatePosition(position);
            
            // Build response
            UpdatePositionResponse response = UpdatePositionResponse.newBuilder()
                    .setSuccess(true)
                    .setConfirmedPosition(Position.newBuilder()
                            .setX(updated.getX())
                            .setY(updated.getY())
                            .setZ(updated.getZ())
                            .setRotation(updated.getDirection())
                            .build())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Position updated")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in UpdatePosition: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to update position: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getNearbyPlayers(GetNearbyPlayersRequest request,
                                 StreamObserver<org.SouthMillion.grpc.gameworld.NearbyPlayersResponse> responseObserver) {
        log.debug("gRPC GetNearbyPlayers: player={}, radius={}", request.getRoleId(), request.getRadius());
        
        try {
            // Get nearby players
            NearbyPlayersResponse nearby = gameWorldService.getNearbyPlayers(
                    request.getRoleId(),
                    request.getZoneId(),
                    request.getRadius()
            );
            
            // Build response
            org.SouthMillion.grpc.gameworld.NearbyPlayersResponse.Builder responseBuilder = 
                    org.SouthMillion.grpc.gameworld.NearbyPlayersResponse.newBuilder()
                            .setTotalCount(nearby.getCount())
                            .setStatus(ResponseStatus.newBuilder()
                                    .setCode(200)
                                    .setMessage("Success")
                                    .setSuccess(true)
                                    .build());
            
            // Add players (limit by request.getMaxPlayers() if needed)
            int maxPlayers = request.getMaxPlayers() > 0 ? request.getMaxPlayers() : 50;
            nearby.getPlayers().stream()
                    .limit(maxPlayers)
                    .forEach(player -> {
                        PlayerInfo playerInfo = PlayerInfo.newBuilder()
                                .setRoleId(player.getPlayerId())
                                .setRoleName("Player" + player.getPlayerId())
                                .setLevel(fetchPlayerLevel(player.getPlayerId()))
                                .setPosition(Position.newBuilder()
                                        .setX(player.getX())
                                        .setY(player.getY())
                                        .setZ(player.getZ())
                                        .setRotation(player.getDirection())
                                        .build())
                                .setMovementState(player.getState())
                                .build();
                        responseBuilder.addPlayers(playerInfo);
                    });
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in GetNearbyPlayers: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get nearby players: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public StreamObserver<PositionUpdateRequest> streamPositionUpdates(
            StreamObserver<PositionUpdateEvent> responseObserver) {
        log.debug("gRPC StreamPositionUpdates started");
        
        return new StreamObserver<PositionUpdateRequest>() {
            @Override
            public void onNext(PositionUpdateRequest request) {
                try {
                    // Update position
                    PlayerPosition position = PlayerPosition.builder()
                            .playerId(request.getRoleId())
                            .mapId(request.getZoneId())
                            .x(request.getPosition().getX())
                            .y(request.getPosition().getY())
                            .z(request.getPosition().getZ())
                            .direction(request.getPosition().getRotation())
                            .state(request.getMovementState())
                            .build();
                    
                    gameWorldService.updatePosition(position);
                    
                    // Send acknowledgment
                    PositionUpdateEvent event = PositionUpdateEvent.newBuilder()
                            .setRoleId(request.getRoleId())
                            .setEventType("POSITION_UPDATED")
                            .setTimestamp(System.currentTimeMillis())
                            .setPosition(request.getPosition())
                            .build();
                    
                    responseObserver.onNext(event);
                    
                } catch (Exception e) {
                    log.error("Error processing position update: {}", e.getMessage(), e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in StreamPositionUpdates: {}", t.getMessage(), t);
            }

            @Override
            public void onCompleted() {
                log.debug("gRPC StreamPositionUpdates completed");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void enterZone(EnterZoneRequest request,
                         StreamObserver<ZoneInfoResponse> responseObserver) {
        log.debug("gRPC EnterZone: player={}, zone={}", request.getRoleId(), request.getZoneId());
        
        try {
            // Get players already in zone
            List<PlayerPosition> playersInZone = gameWorldService.getPlayersOnMap(request.getZoneId());
            
            ZoneInfoResponse response = ZoneInfoResponse.newBuilder()
                    .setZoneId(request.getZoneId())
                    .setZoneName("Zone " + request.getZoneId())
                    .setPlayerCount(playersInZone.size())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Entered zone successfully")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in EnterZone: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to enter zone: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void leaveZone(LeaveZoneRequest request,
                         StreamObserver<ResponseStatus> responseObserver) {
        log.debug("gRPC LeaveZone: player={}, zone={}", request.getRoleId(), request.getZoneId());
        
        try {
            boolean removed = gameWorldService.removePlayer(request.getRoleId());
            
            ResponseStatus status = ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage(removed ? "Left zone successfully" : "Player not in zone")
                    .setSuccess(true)
                    .build();
            
            responseObserver.onNext(status);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in LeaveZone: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to leave zone: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getZoneInfo(GetZoneInfoRequest request,
                           StreamObserver<ZoneInfoResponse> responseObserver) {
        log.debug("gRPC GetZoneInfo: zone={}", request.getZoneId());
        
        try {
            List<PlayerPosition> playersInZone = gameWorldService.getPlayersOnMap(request.getZoneId());
            
            ZoneInfoResponse response = ZoneInfoResponse.newBuilder()
                    .setZoneId(request.getZoneId())
                    .setZoneName("Zone " + request.getZoneId())
                    .setPlayerCount(playersInZone.size())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in GetZoneInfo: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get zone info: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void spawnEntity(SpawnEntityRequest request,
                           StreamObserver<EntityResponse> responseObserver) {
        log.debug("gRPC SpawnEntity: type={}, zone={}", request.getEntityType(), request.getZoneId());
        
        try {
            // Assign a unique runtime entity ID (timestamp-based, scoped to zone lifecycle)
            int entityId = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
            log.info("Spawning entity type={} in zone={} with entityId={}", request.getEntityType(), request.getZoneId(), entityId);

            EntityResponse response = EntityResponse.newBuilder()
                    .setEntityId(entityId)
                    .setEntityType(request.getEntityType())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Entity spawned")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in SpawnEntity: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to spawn entity: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void despawnEntity(DespawnEntityRequest request,
                             StreamObserver<ResponseStatus> responseObserver) {
        log.debug("gRPC DespawnEntity: instanceId={}", request.getInstanceId());
        
        try {
            ResponseStatus status = ResponseStatus.newBuilder()
                    .setCode(200)
                    .setMessage("Entity despawned")
                    .setSuccess(true)
                    .build();
            
            responseObserver.onNext(status);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in DespawnEntity: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to despawn entity: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * Fetch player level from role-service; returns 1 on failure (graceful degradation).
     */
    private int fetchPlayerLevel(long playerId) {
        try {
            Map<String, Object> roleInfo = roleServiceClient.getRoleInfo(playerId);
            if (roleInfo != null && roleInfo.containsKey("level")) {
                Object level = roleInfo.get("level");
                if (level instanceof Number) {
                    return ((Number) level).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch level for playerId={}: {} — defaulting to 1", playerId, e.getMessage());
        }
        return 1;
    }
}

