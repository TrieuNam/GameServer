package com.SouthMillion.mount_service.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.mount_service.event.MountGradeUpEvent;
import com.SouthMillion.mount_service.event.MountLevelUpEvent;
import com.SouthMillion.mount_service.event.MountUnlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MountEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_MOUNT_UNLOCKED = "mount.unlocked";
    private static final String TOPIC_MOUNT_LEVEL_UP = "mount.level.up";
    private static final String TOPIC_MOUNT_GRADE_UP = "mount.grade.up";

    public void publishMountUnlocked(Long userId, Integer mountId, int mountIndex) {
        try {
            MountUnlockedEvent event = MountUnlockedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(String.valueOf(userId))
                    .mountId(mountId)
                    .mountIndex(mountIndex)
                    .timestamp(System.currentTimeMillis())
                    .source("mount-service")
                    .build();

            String message = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(TOPIC_MOUNT_UNLOCKED, String.valueOf(userId), message);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[MountEventPublisher] Failed to publish mount.unlocked: userId={}, mountId={}", 
                            userId, mountId, ex);
                } else {
                    log.info("[MountEventPublisher] Mount unlocked published: userId={}, mountId={}, index={}", 
                            userId, mountId, mountIndex);
                }
            });
            
        } catch (Exception e) {
            log.error("[MountEventPublisher] Error creating mount.unlocked event", e);
        }
    }

    public void publishMountLevelUp(Long userId, Integer mountId, Integer mountIndex,
                                    Integer oldLevel, Integer newLevel, Long expGained) {
        try {
            MountLevelUpEvent event = MountLevelUpEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(String.valueOf(userId))
                    .mountId(mountId)
                    .mountIndex(mountIndex)
                    .oldLevel(oldLevel)
                    .newLevel(newLevel)
                    .levelsGained(newLevel - oldLevel)
                    .expGained(expGained)
                    .timestamp(System.currentTimeMillis())
                    .source("mount-service")
                    .build();

            String message = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(TOPIC_MOUNT_LEVEL_UP, String.valueOf(userId), message);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[MountEventPublisher] Failed to publish mount.level.up: userId={}, mountId={}", 
                            userId, mountId, ex);
                } else {
                    log.info("[MountEventPublisher] Mount level up published: userId={}, mountId={}, {} -> {}", 
                            userId, mountId, oldLevel, newLevel);
                }
            });
            
        } catch (Exception e) {
            log.error("[MountEventPublisher] Error creating mount.level.up event", e);
        }
    }

    public void publishMountGradeUp(Long userId, Integer mountId, Integer mountIndex,
                                    Integer oldGrade, Integer newGrade, Integer mountLevel) {
        try {
            MountGradeUpEvent event = MountGradeUpEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(String.valueOf(userId))
                    .mountId(mountId)
                    .mountIndex(mountIndex)
                    .oldGrade(oldGrade)
                    .newGrade(newGrade)
                    .mountLevel(mountLevel)
                    .timestamp(System.currentTimeMillis())
                    .source("mount-service")
                    .build();

            String message = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(TOPIC_MOUNT_GRADE_UP, String.valueOf(userId), message);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[MountEventPublisher] Failed to publish mount.grade.up: userId={}, mountId={}", 
                            userId, mountId, ex);
                } else {
                    log.info("[MountEventPublisher] Mount grade up published: userId={}, mountId={}, {} -> {}", 
                            userId, mountId, oldGrade, newGrade);
                }
            });
            
        } catch (Exception e) {
            log.error("[MountEventPublisher] Error creating mount.grade.up event", e);
        }
    }
}
