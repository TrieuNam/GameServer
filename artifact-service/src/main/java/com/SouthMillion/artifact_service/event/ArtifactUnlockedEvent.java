package com.SouthMillion.artifact_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactUnlockedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer artifactId;
    private Integer artifactIndex;
    private Long timestamp = System.currentTimeMillis();
    private String source = "artifact-service";
    
    public ArtifactUnlockedEvent(Long userId, Integer artifactId, Integer artifactIndex) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.artifactIndex = artifactIndex;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
