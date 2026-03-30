package com.SouthMillion.arenaservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.arena.ArenaMatchEndEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Publisher for Arena Service
 * Publishes arena match events to enable ranking, achievements, statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_MATCH_END = "arena.match.end";

    /**
     * Publish arena match end event
     */
    public void publishMatchEnd(Long matchId, Long winnerId, String winnerName, 
                                Integer winnerRatingBefore, Integer winnerRatingAfter,
                                Long loserId, String loserName,
                                Integer loserRatingBefore, Integer loserRatingAfter,
                                Integer matchDuration, String matchType, Integer seasonId,
                                Integer totalDamageWinner, Integer totalDamageLoser,
                                Boolean isPerfectVictory) {
        try {
            ArenaMatchEndEvent event = ArenaMatchEndEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .matchId(matchId)
                    .winnerId(winnerId)
                    .winnerName(winnerName)
                    .winnerRatingBefore(winnerRatingBefore)
                    .winnerRatingAfter(winnerRatingAfter)
                    .loserId(loserId)
                    .loserName(loserName)
                    .loserRatingBefore(loserRatingBefore)
                    .loserRatingAfter(loserRatingAfter)
                    .matchDuration(matchDuration)
                    .matchType(matchType)
                    .seasonId(seasonId)
                    .totalDamageWinner(totalDamageWinner)
                    .totalDamageLoser(totalDamageLoser)
                    .isPerfectVictory(isPerfectVictory)
                    .matchEndAt(Instant.now())
                    .build();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    TOPIC_MATCH_END,
                    winnerId.toString(),
                    event
            );
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[kafka-arena] Published ArenaMatchEndEvent: matchId={}, winner={}, loser={}", 
                            matchId, winnerId, loserId);
                } else {
                    log.error("[kafka-arena] Failed to publish ArenaMatchEndEvent: {}", ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("[kafka-arena] Error publishing ArenaMatchEndEvent: {}", e.getMessage(), e);
        }
    }
}
