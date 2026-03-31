package com.SouthMillion.artifact_service.controller;

import com.SouthMillion.artifact_service.model.entity.Artifact;
import com.SouthMillion.artifact_service.model.entity.ArtifactDrawRecord;
import com.SouthMillion.artifact_service.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Artifact Controller
 * Divine weapon/artifact (ShenQi) operations
 * 
 * MsgIDs: 1675-1680
 */
@RestController
@RequestMapping("/api/artifact")
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {
    
    private final ArtifactService artifactService;
    
    @GetMapping("/{roleId}")
    public ResponseEntity<List<Artifact>> getAllArtifacts(@PathVariable Long roleId) {
        log.debug("GET /api/artifact/{}", roleId);
        List<Artifact> artifacts = artifactService.getAllArtifacts(roleId);
        return ResponseEntity.ok(artifacts);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}")
    public ResponseEntity<Artifact> getArtifact(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        log.debug("GET /api/artifact/{}/{}", roleId, artifactIndex);
        Artifact artifact = artifactService.getArtifact(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/unlock")
    public ResponseEntity<Artifact> unlockArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactId) {
        log.info("POST /api/artifact/{}/unlock - artifactId={}", roleId, artifactId);
        Artifact artifact = artifactService.unlockArtifact(roleId, artifactId);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/levelup")
    public ResponseEntity<Artifact> levelUpArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/levelup - artifactIndex={}", roleId, artifactIndex);
        Artifact artifact = artifactService.levelUpArtifact(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/gradeup")
    public ResponseEntity<Artifact> gradeUpArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/gradeup - artifactIndex={}", roleId, artifactIndex);
        Artifact artifact = artifactService.gradeUpArtifact(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/equip")
    public ResponseEntity<Void> equipArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/equip - artifactIndex={}", roleId, artifactIndex);
        artifactService.equipArtifact(roleId, artifactIndex);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{roleId}/equip")
    public ResponseEntity<Void> unequipArtifact(@PathVariable Long roleId) {
        log.info("DELETE /api/artifact/{}/equip", roleId);
        artifactService.unequipArtifact(roleId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roleId}/refine")
    public ResponseEntity<Artifact> refineArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/refine - artifactIndex={}", roleId, artifactIndex);
        Artifact artifact = artifactService.refineArtifact(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/awaken")
    public ResponseEntity<Artifact> awakenArtifact(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/awaken - artifactIndex={}", roleId, artifactIndex);
        Artifact artifact = artifactService.awakenArtifact(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/soulpower")
    public ResponseEntity<Artifact> addSoulPower(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex,
            @RequestParam Long points) {
        log.info("POST /api/artifact/{}/soulpower - artifactIndex={}, points={}", 
            roleId, artifactIndex, points);
        Artifact artifact = artifactService.addSoulPower(roleId, artifactIndex, points);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/essence")
    public ResponseEntity<Artifact> addDivineEssence(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex,
            @RequestParam Long amount) {
        log.info("POST /api/artifact/{}/essence - artifactIndex={}, amount={}", 
            roleId, artifactIndex, amount);
        Artifact artifact = artifactService.addDivineEssence(roleId, artifactIndex, amount);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/blessing")
    public ResponseEntity<Artifact> upgradeBlessing(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex) {
        log.info("POST /api/artifact/{}/blessing - artifactIndex={}", roleId, artifactIndex);
        Artifact artifact = artifactService.upgradeBlessing(roleId, artifactIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/refresh")
    public ResponseEntity<Artifact> refreshAttributes(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex,
            @RequestParam(defaultValue = "0") Integer lockFlag) {
        log.info("POST /api/artifact/{}/refresh - artifactIndex={}, lockFlag={}", 
            roleId, artifactIndex, lockFlag);
        Artifact artifact = artifactService.refreshAttributes(roleId, artifactIndex, lockFlag);
        return ResponseEntity.ok(artifact);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}/power")
    public ResponseEntity<Long> getArtifactPower(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        Artifact artifact = artifactService.getArtifact(roleId, artifactIndex);
        Long power = artifactService.calculateArtifactPower(artifact);
        return ResponseEntity.ok(power);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}/canlevelup")
    public ResponseEntity<Boolean> canLevelUp(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        boolean can = artifactService.canLevelUp(roleId, artifactIndex);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}/cangradeup")
    public ResponseEntity<Boolean> canGradeUp(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        boolean can = artifactService.canGradeUp(roleId, artifactIndex);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}/canrefine")
    public ResponseEntity<Boolean> canRefine(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        boolean can = artifactService.canRefine(roleId, artifactIndex);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{roleId}/{artifactIndex}/canawaken")
    public ResponseEntity<Boolean> canAwaken(
            @PathVariable Long roleId,
            @PathVariable Integer artifactIndex) {
        boolean can = artifactService.canAwaken(roleId, artifactIndex);
        return ResponseEntity.ok(can);
    }
    
    @PostMapping("/{roleId}/upgrade-skill")
    public ResponseEntity<Artifact> upgradeArtifactSkill(
            @PathVariable Long roleId,
            @RequestParam Integer artifactIndex,
            @RequestParam Integer skillIndex) {
        log.info("POST /api/artifact/{}/upgrade-skill - artifactIndex={}, skillIndex={}", 
                roleId, artifactIndex, skillIndex);
        Artifact artifact = artifactService.upgradeArtifactSkill(roleId, artifactIndex, skillIndex);
        return ResponseEntity.ok(artifact);
    }
    
    @PostMapping("/{roleId}/draw")
    public ResponseEntity<List<Map<String, Object>>> drawArtifacts(
            @PathVariable Long roleId,
            @RequestParam Integer drawType) {
        log.info("POST /api/artifact/{}/draw - drawType={}", roleId, drawType);
        List<Map<String, Object>> results = artifactService.drawArtifacts(roleId, drawType);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{roleId}/draw-records")
    public ResponseEntity<List<ArtifactDrawRecord>> getDrawRecords(@PathVariable Long roleId) {
        log.info("GET /api/artifact/{}/draw-records", roleId);
        List<ArtifactDrawRecord> records = artifactService.getDrawRecords(roleId);
        return ResponseEntity.ok(records);
    }
}
