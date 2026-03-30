package com.SouthMillion.artifact_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactAwakenedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer artifactId;
    private Integer artifactIndex;
    private Integer oldStage;
    private Integer newStage;
    private Long timestamp = System.currentTimeMillis();
    private String source = "artifact-service";
    
    public ArtifactAwakenedEvent(Long userId, Integer artifactId, Integer artifactIndex, 
                                Integer oldStage, Integer newStage) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.artifactIndex = artifactIndex;
        this.oldStage = oldStage;
        this.newStage = newStage;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
