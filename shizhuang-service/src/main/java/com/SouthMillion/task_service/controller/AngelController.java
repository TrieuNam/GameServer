package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.AngelService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.AngelConfigDTO;
import org.SouthMillion.dto.ShiZhuang.PlayerAngelDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// AngelController disabled in shizhuang-service (belongs to angel-service)
// @RestController
@RequestMapping("/api/angel")
@RequiredArgsConstructor
public class AngelController {
    private final AngelService angelService;

    @PostMapping("/{playerId}/levelup")
    public ResponseEntity<?> levelUpAngel(@PathVariable Long playerId) {
        angelService.levelUpAngel(playerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{playerId}/stage-cfg")
    public ResponseEntity<AngelConfigDTO.AngelUpCfg> getStageCfg(@PathVariable Long playerId) {
        return ResponseEntity.ok(angelService.getCurrentAngelStageCfg(playerId));
    }

    /**
     * API lấy thông tin thiên sứ của player (PlayerAngelDTO)
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerAngelDTO> getAngelInfo(@PathVariable Long playerId) {
        PlayerAngelDTO dto = angelService.getAngelInfoDTO(playerId);
        return ResponseEntity.ok(dto);
    }

    /**
     * API tăng bậc thiên sứ cho player
     */
    @PostMapping("/{playerId}/gradeup")
    public ResponseEntity<?> gradeUpAngel(@PathVariable Long playerId) {
        angelService.gradeUpAngel(playerId);
        return ResponseEntity.ok().build();
    }
}