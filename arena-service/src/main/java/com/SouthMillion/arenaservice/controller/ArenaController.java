package com.SouthMillion.arenaservice.controller;

import com.SouthMillion.arenaservice.dto.ArenaPlayerDTO;
import com.SouthMillion.arenaservice.dto.BattleRequest;
import com.SouthMillion.arenaservice.dto.BattleResult;
import com.SouthMillion.arenaservice.entity.ArenaBattleHistory;
import com.SouthMillion.arenaservice.entity.ArenaPlayer;
import com.SouthMillion.arenaservice.service.ArenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arena")
@RequiredArgsConstructor
public class ArenaController {

    private final ArenaService arenaService;

    @PostMapping("/{playerId}/enter")
    public ResponseEntity<?> enterArena(@PathVariable String playerId) {
        try {
            ArenaPlayer player = arenaService.getOrCreatePlayer(playerId);
            ArenaPlayerDTO playerDTO = arenaService.toDTO(player);
            
            // Find opponent
            ArenaPlayer opponent = arenaService.findOpponent(playerId).orElse(null);
            
            return ResponseEntity.ok(Map.of(
                    "player", playerDTO,
                    "opponent", opponent != null ? arenaService.toDTO(opponent) : null,
                    "message", opponent != null ? "Opponent found" : "No opponent available"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{playerId}/battle")
    public ResponseEntity<?> startBattle(@PathVariable String playerId, @RequestBody(required = false) BattleRequest request) {
        try {
            if (request == null) {
                request = BattleRequest.builder().playerId(playerId).build();
            } else {
                request.setPlayerId(playerId);
            }
            
            BattleResult result = arenaService.processBattle(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Battle processing failed"));
        }
    }

    @GetMapping("/{playerId}/opponent")
    public ResponseEntity<?> getOpponent(@PathVariable String playerId) {
        ArenaPlayer opponent = arenaService.findOpponent(playerId).orElse(null);
        
        if (opponent == null) {
            return ResponseEntity.ok(Map.of("message", "No opponent available"));
        }
        
        return ResponseEntity.ok(arenaService.toDTO(opponent));
    }

    @GetMapping("/rankings")
    public ResponseEntity<List<ArenaPlayerDTO>> getRankings(
            @RequestParam(defaultValue = "1") Integer season) {
        List<ArenaPlayerDTO> rankings = arenaService.getTop100Rankings(season);
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/rankings/page")
    public ResponseEntity<Page<ArenaPlayerDTO>> getRankingsPage(
            @RequestParam(defaultValue = "1") Integer season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ArenaPlayerDTO> rankings = arenaService.getRankings(season, page, size);
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/{playerId}/rank")
    public ResponseEntity<?> getPlayerRank(@PathVariable String playerId) {
        try {
            Integer rank = arenaService.calculatePlayerRank(playerId);
            return ResponseEntity.ok(Map.of("playerId", playerId, "rank", rank));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{playerId}/rating")
    public ResponseEntity<?> getPlayerRating(@PathVariable String playerId) {
        return arenaService.getPlayer(playerId)
                .map(player -> ResponseEntity.ok(Map.of(
                        "playerId", playerId,
                        "rating", player.getRating(),
                        "wins", player.getWins(),
                        "losses", player.getLosses(),
                        "winRate", player.getWinRate()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{playerId}/history")
    public ResponseEntity<Page<ArenaBattleHistory>> getBattleHistory(
            @PathVariable String playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArenaBattleHistory> history = arenaService.getPlayerBattleHistory(playerId, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<?> getPlayerStats(@PathVariable String playerId) {
        return arenaService.getPlayer(playerId)
                .map(arenaService::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
