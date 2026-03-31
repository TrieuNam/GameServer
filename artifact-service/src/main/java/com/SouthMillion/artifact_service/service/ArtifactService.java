package com.SouthMillion.artifact_service.service;

import com.SouthMillion.artifact_service.model.entity.Artifact;
import com.SouthMillion.artifact_service.model.entity.ArtifactDrawRecord;

import java.util.List;
import java.util.Map;

public interface ArtifactService {
    
    List<Artifact> getAllArtifacts(Long userId);
    
    Artifact getArtifact(Long userId, Integer artifactIndex);
    
    Artifact unlockArtifact(Long userId, Integer artifactId);
    
    Artifact levelUpArtifact(Long userId, Integer artifactIndex);
    
    Artifact gradeUpArtifact(Long userId, Integer artifactIndex);
    
    void equipArtifact(Long userId, Integer artifactIndex);
    
    void unequipArtifact(Long userId);
    
    Artifact refineArtifact(Long userId, Integer artifactIndex);
    
    Artifact awakenArtifact(Long userId, Integer artifactIndex);
    
    Artifact addSoulPower(Long userId, Integer artifactIndex, Long points);
    
    Artifact addDivineEssence(Long userId, Integer artifactIndex, Long amount);
    
    Artifact upgradeBlessing(Long userId, Integer artifactIndex);
    
    Artifact refreshAttributes(Long userId, Integer artifactIndex, Integer lockFlag);
    
    Long calculateArtifactPower(Artifact artifact);
    
    boolean canLevelUp(Long userId, Integer artifactIndex);
    
    boolean canGradeUp(Long userId, Integer artifactIndex);
    
    boolean canRefine(Long userId, Integer artifactIndex);
    
    boolean canAwaken(Long userId, Integer artifactIndex);
    
    Artifact upgradeArtifactSkill(Long userId, Integer artifactIndex, Integer skillIndex);
    
    /**
     * Draw artifacts from gacha (1=single, 10=10-pull)
     * Returns list of drawn artifact IDs with quality info
     */
    List<Map<String, Object>> drawArtifacts(Long userId, Integer drawType);
    
    /**
     * Get recent draw history (last 100 records)
     */
    List<ArtifactDrawRecord> getDrawRecords(Long userId);
}
