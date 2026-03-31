package org.SouthMillion.dto.event.arena;

import lombok.*;

import java.time.Instant;

/**
 * Arena Match End Event - Published when arena match ends
 * Consumed by: ranking-service, achievement-service, statistics-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArenaMatchEndEvent {
    private String eventId;
    private Long matchId;
    
    // Winner info
    private Long winnerId;
    private String winnerName;
    private Integer winnerRatingBefore;
    private Integer winnerRatingAfter;
    
    // Loser info
    private Long loserId;
    private String loserName;
    private Integer loserRatingBefore;
    private Integer loserRatingAfter;
    
    // Match details
    private Integer matchDuration;  // seconds
    private String matchType;       // "RANKED", "CASUAL", "TOURNAMENT"
    private Integer seasonId;
    private Instant matchEndAt;
    
    // Stats
    private Integer totalDamageWinner;
    private Integer totalDamageLoser;
    private Boolean isPerfectVictory;  // No damage taken
}
