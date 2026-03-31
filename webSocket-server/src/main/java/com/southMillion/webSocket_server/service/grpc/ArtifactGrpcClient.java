package com.SouthMillion.webSocket_server.service.grpc;

import com.SouthMillion.webSocket_server.service.client.ArtifactFeign;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.artifact.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for artifact-service (ShenQi/Divine Artifact).
 * Uses gRPC stub (artifact_service.proto) with REST fallback via ArtifactFeign.
 */
@Slf4j
@Service
public class ArtifactGrpcClient {

    @GrpcClient("artifact-service")
    private ArtifactGrpcServiceGrpc.ArtifactGrpcServiceBlockingStub artifactStub;

    @Autowired(required = false)
    private ArtifactFeign artifactFeign;

    /** Get all artifacts for a role */
    public Object getRoleArtifacts(Long roleId) {
        try {
            log.debug("[grpc-artifact] getAllArtifacts: roleId={}", roleId);
            GetAllArtifactsResponse resp = artifactStub.getAllArtifacts(
                    GetAllArtifactsRequest.newBuilder().setUserId(roleId).build());
            return resp.getArtifactsList();
        } catch (Exception e) {
            log.warn("[grpc-artifact] gRPC getAllArtifacts failed, falling back to REST: {}", e.getMessage());
            if (artifactFeign != null) {
                return artifactFeign.getRoleArtifacts(roleId);
            }
            return null;
        }
    }

    /** Unlock/activate an artifact by template id */
    public Object activateArtifact(Long roleId, int artifactId) {
        try {
            log.debug("[grpc-artifact] unlockArtifact: roleId={}, artifactId={}", roleId, artifactId);
            UnlockArtifactResponse resp = artifactStub.unlockArtifact(
                    UnlockArtifactRequest.newBuilder()
                            .setUserId(roleId)
                            .setArtifactId(artifactId)
                            .build());
            return resp.getArtifact();
        } catch (Exception e) {
            log.warn("[grpc-artifact] gRPC unlockArtifact failed, falling back to REST: {}", e.getMessage());
            if (artifactFeign != null) {
                return artifactFeign.activateArtifact(roleId, artifactId);
            }
            return null;
        }
    }

    /** Level up an artifact by artifact index */
    public Object upgradeArtifact(Long roleId, Long artifactIndex, int targetLevel) {
        try {
            int idx = artifactIndex != null ? artifactIndex.intValue() : 0;
            log.debug("[grpc-artifact] levelUpArtifact: roleId={}, artifactIndex={}", roleId, idx);
            LevelUpArtifactResponse resp = artifactStub.levelUpArtifact(
                    LevelUpArtifactRequest.newBuilder()
                            .setUserId(roleId)
                            .setArtifactIndex(idx)
                            .build());
            return resp.getArtifact();
        } catch (Exception e) {
            log.warn("[grpc-artifact] gRPC levelUpArtifact failed, falling back to REST: {}", e.getMessage());
            if (artifactFeign != null) {
                return artifactFeign.upgradeArtifact(roleId,
                        artifactIndex != null ? artifactIndex.intValue() : 0);
            }
            return null;
        }
    }
}

