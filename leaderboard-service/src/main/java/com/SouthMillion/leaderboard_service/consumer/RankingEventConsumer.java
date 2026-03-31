package com.SouthMillion.leaderboard_service.consumer;

import com.SouthMillion.leaderboard_service.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.arena.ArenaMatchEndEvent;
import org.SouthMillion.dto.event.trial.TrialCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Ranking Event Consumer - Updates leaderboards from game events
 * Listens to: arena.match.end, trial.completed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventConsumer {

    private final RankingService rankingService;

    /**
     * Handle arena match end events for PvP ranking updates
     */
    @KafkaListener(topics = "arena.match.end", groupId = "leaderboard-service")
    public void handleArenaMatchEnd(
            @Payload ArenaMatchEndEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack
    ) {
        try {
            log.info("Received arena match end for ranking: matchId={}, winner={}, loser={}", 
                    event.getMatchId(), event.getWinnerName(), event.getLoserName());

            // Update arena rankings for both players
            rankingService.updateArenaRanking(
                    String.valueOf(event.getWinnerId()),
                    event.getWinnerName(),
                    event.getWinnerRatingAfter(),
                    true // is winner
            );

            rankingService.updateArenaRanking(
                    String.valueOf(event.getLoserId()),
                    event.getLoserName(),
                    event.getLoserRatingAfter(),
                    false // is loser
            );

            // Update season-specific rankings if needed
            if (event.getSeasonId() != null) {
                rankingService.updateSeasonRanking(
                        String.valueOf(event.getWinnerId()),
                        event.getSeasonId().toString(),
                        event.getWinnerRatingAfter()
                );
                rankingService.updateSeasonRanking(
                        String.valueOf(event.getLoserId()),
                        event.getSeasonId().toString(),
                        event.getLoserRatingAfter()
                );
            }

            ack.acknowledge();
            log.debug("Successfully updated arena rankings for match: {}", event.getMatchId());
        } catch (Exception e) {
            log.error("Error updating arena rankings for match: {}", event.getMatchId(), e);
            throw new RuntimeException("Failed to update arena rankings", e);
        }
    }

    /**
     * Handle trial completion events for PvE ranking updates
     */
    @KafkaListener(topics = "trial.completed", groupId = "leaderboard-service")
    public void handleTrialCompleted(
            @Payload TrialCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack
    ) {
        try {
            log.info("Received trial completion for ranking: roleId={}, trialId={}, score={}, stars={}", 
                    event.getRoleId(), event.getTrialId(), event.getScore(), event.getStars());

            // Update trial rankings
            rankingService.updateTrialRanking(
                    String.valueOf(event.getRoleId()),
                    event.getTrialId(),
                    event.getScore(),
                    event.getStars(),
                    event.getCompletionTime()
            );

            // Update global trial score ranking
            rankingService.updateGlobalTrialRanking(
                    String.valueOf(event.getRoleId()),
                    event.getScore()
            );

            ack.acknowledge();
            log.debug("Successfully updated trial rankings for roleId: {}", event.getRoleId());
        } catch (Exception e) {
            log.error("Error updating trial rankings for roleId: {}", event.getRoleId(), e);
            throw new RuntimeException("Failed to update trial rankings", e);
        }
    }
}
