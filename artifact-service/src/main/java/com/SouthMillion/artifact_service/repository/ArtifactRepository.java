package com.SouthMillion.artifact_service.repository;

import com.SouthMillion.artifact_service.model.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    
    List<Artifact> findByUserId(Long userId);
    
    Optional<Artifact> findByUserIdAndArtifactIndex(Long userId, Integer artifactIndex);
    
    Optional<Artifact> findByUserIdAndIsEquippedTrue(Long userId);
    
    List<Artifact> findByUserIdAndIsActiveTrue(Long userId);
    
    long countByUserIdAndIsActiveTrue(Long userId);
    
    @Modifying
    @Query("UPDATE Artifact a SET a.isEquipped = false WHERE a.userId = :userId AND a.isEquipped = true")
    void unequipAllArtifacts(@Param("userId") Long userId);
    
    boolean existsByUserIdAndArtifactIndex(Long userId, Integer artifactIndex);
    
    Optional<Artifact> findByUserIdAndArtifactId(Long userId, Integer artifactId);
}
