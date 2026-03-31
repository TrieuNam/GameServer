package org.SouthMillion.dto.event.trial;

import lombok.*;

import java.time.Instant;

/**
 * Trial Failed Event - Published when player fails a trial
 * Consumed by: statistics-service, analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialFailedEvent {
    private String eventId;
    private Long userId;
    private Long roleId;
    private Integer trialId;
    private Integer stageReached;
    private String failReason;   // "TIMEOUT", "DEFEATED", "ABANDONED"
    private Integer attempts;
    private Instant failedAt;
}
