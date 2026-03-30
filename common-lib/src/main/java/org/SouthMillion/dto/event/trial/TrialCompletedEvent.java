package org.SouthMillion.dto.event.trial;

import lombok.*;

import java.time.Instant;

/**
 * Trial Completed Event - Published when player completes a trial
 * Consumed by: achievement-service, statistics-service, ranking-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialCompletedEvent {
    private String eventId;      // UUID for idempotency
    private Long userId;
    private Long roleId;
    private Integer trialId;
    private Integer stageReached;
    private Long score;
    private Integer stars;
    private Integer completionTime; // seconds
    private Boolean isNewRecord;    // Beat previous best
    private Instant completedAt;
    
    // Additional context
    private String source;       // "NORMAL", "DAILY", "EVENT"
    private Integer attempts;    // Number of attempts today
}
