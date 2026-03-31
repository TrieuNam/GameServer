package com.SouthMillion.task_service.consumer;

import com.SouthMillion.task_service.service.AchievementService;
import com.SouthMillion.task_service.service.StatisticsService;
import com.SouthMillion.task_service.client.LeaderboardClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.arena.ArenaMatchEndEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes arena events for task/achievement tracking
 * Listens to: arena.match.end
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArenaEventConsumer {

    private final AchievementService achievementService;
    private final StatisticsService statisticsService;
    private final LeaderboardClient leaderboardClient;

    /**
     * Handle arena match end events
     * - Update task progress (e.g., "Win 10 Arena matches")
     * - Track achievements (e.g., "Arena Champion", "Perfect Victory")
     * - Update rankings and statistics
     */
    @KafkaListener(topics = "arena.match.end", groupId = "task-service")
    public void handleArenaMatchEnd(
            @Payload ArenaMatchEndEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            log.info("Received arena match end event: eventId={}, matchId={}, winner={}, loser={}, duration={}s", 
                    event.getEventId(), event.getMatchId(), event.getWinnerName(), 
                    event.getLoserName(), event.getMatchDuration());

            // Process winner achievements and stats
            int winnerRatingChange = event.getWinnerRatingAfter() - event.getWinnerRatingBefore();
            
            // Track win streak from statistics
            int winStreak = 0;
            StatisticsService.PlayerStatistics stats = statisticsService.getStats(String.valueOf(event.getWinnerId()));
            if (stats != null) {
                winStreak = stats.getCurrentWinStreak() + 1;
            }
            achievementService.checkArenaAchievements(
                    String.valueOf(event.getWinnerId()),
                    true, // is winner
                    event.getIsPerfectVictory() != null && event.getIsPerfectVictory(),
                    winStreak,
                    event.getTotalDamageWinner() != null ? event.getTotalDamageWinner().longValue() : 0L
            );
            statisticsService.updateArenaStats(
                    String.valueOf(event.getWinnerId()),
                    true, // is winner
                    event.getIsPerfectVictory() != null && event.getIsPerfectVictory(),
                    winnerRatingChange,
                    event.getMatchDuration().longValue(),
                    event.getTotalDamageWinner() != null ? event.getTotalDamageWinner().longValue() : 0L
            );
            
            // Process loser stats
            int loserRatingChange = event.getLoserRatingAfter() - event.getLoserRatingBefore();
            statisticsService.updateArenaStats(
                    String.valueOf(event.getLoserId()),
                    false, // is loser
                    false, // not perfect
                    loserRatingChange,
                    event.getMatchDuration().longValue(),
                    event.getTotalDamageLoser() != null ? event.getTotalDamageLoser().longValue() : 0L
            );
            achievementService.checkArenaAchievements(
                    String.valueOf(event.getLoserId()),
                    false, // is loser
                    false,
                    0,
                    0L
            );
            
            // Task progress (arena_win) is now handled by analytics-service
            // via task.progress.update topic → TaskProgressEventConsumer

            // Update ranking/leaderboard service for winner
            String winnerRoleId = String.valueOf(event.getWinnerId());
            try {
                leaderboardClient.updateScore(LeaderboardClient.UpdateScoreRequest.builder()
                        .rankingType(LeaderboardClient.RANKING_TYPE_ARENA)
                        .roleId(winnerRoleId)
                        .roleName(event.getWinnerName())
                        .roleLevel(1) // level not in event; use 1 as safe default
                        .score((long) event.getWinnerRatingAfter())
                        .build());
                log.debug("[arena] Updated leaderboard for winner={}, rating={}",
                        winnerRoleId, event.getWinnerRatingAfter());
            } catch (Exception e) {
                log.warn("[arena] Failed to update leaderboard for winner={}: {}",
                        winnerRoleId, e.getMessage());
            }

            // Update ranking/leaderboard service for loser
            try {
                leaderboardClient.updateScore(LeaderboardClient.UpdateScoreRequest.builder()
                        .rankingType(LeaderboardClient.RANKING_TYPE_ARENA)
                        .roleId(String.valueOf(event.getLoserId()))
                        .roleName(event.getLoserName())
                        .roleLevel(1)
                        .score((long) event.getLoserRatingAfter())
                        .build());
                log.debug("[arena] Updated leaderboard for loser={}, rating={}",
                        event.getLoserId(), event.getLoserRatingAfter());
            } catch (Exception e) {
                log.warn("[arena] Failed to update leaderboard for loser={}: {}",
                        event.getLoserId(), e.getMessage());
            }

            log.debug("Successfully processed arena match end event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing arena match end event: {}", event.getEventId(), e);
        }
    }
}
