package com.SouthMillion.worlds_ervice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.worlds_ervice.dto.WorldBossDTO;
import com.SouthMillion.worlds_ervice.dto.WorldEventDTO;
import com.SouthMillion.worlds_ervice.dto.WorldStateDTO;
import com.SouthMillion.worlds_ervice.entity.WorldBoss;
import com.SouthMillion.worlds_ervice.entity.WorldEvent;
import com.SouthMillion.worlds_ervice.entity.WorldState;
import com.SouthMillion.worlds_ervice.repository.WorldBossRepository;
import com.SouthMillion.worlds_ervice.repository.WorldEventRepository;
import com.SouthMillion.worlds_ervice.repository.WorldStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldService {

    private final WorldStateRepository worldStateRepository;
    private final WorldEventRepository worldEventRepository;
    private final WorldBossRepository worldBossRepository;
    private final ObjectMapper objectMapper;

    private static final String GLOBAL_STATE_KEY = "GLOBAL_STATE";

    @Cacheable(value = "worldState", key = "'global'")
    public WorldStateDTO getGlobalState() {
        WorldState state = worldStateRepository.findByStateKey(GLOBAL_STATE_KEY)
                .orElseGet(this::createDefaultGlobalState);

        try {
            Map<String, Object> stateData = objectMapper.readValue(
                    state.getStateData(), 
                    new TypeReference<Map<String, Object>>() {}
            );
            
            return WorldStateDTO.builder()
                    .stateKey(state.getStateKey())
                    .stateData(stateData)
                    .version(state.getVersion())
                    .serverTime(Instant.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse state data: {}", e.getMessage());
            return WorldStateDTO.builder()
                    .stateKey(GLOBAL_STATE_KEY)
                    .stateData(Map.of("error", "Failed to load state"))
                    .serverTime(Instant.now())
                    .build();
        }
    }

    @Transactional
    @CacheEvict(value = "worldState", allEntries = true)
    public WorldState updateGlobalState(Map<String, Object> stateData) {
        WorldState state = worldStateRepository.findByStateKey(GLOBAL_STATE_KEY)
                .orElseGet(this::createDefaultGlobalState);

        try {
            String stateJson = objectMapper.writeValueAsString(stateData);
            state.setStateData(stateJson);
            state.setVersion(state.getVersion() + 1);
            return worldStateRepository.save(state);
        } catch (Exception e) {
            log.error("Failed to update state: {}", e.getMessage());
            throw new RuntimeException("Failed to update world state");
        }
    }

    @Cacheable(value = "activeEvents")
    public List<WorldEventDTO> getActiveEvents() {
        Instant now = Instant.now();
        return worldEventRepository.findActiveEventsAt(now).stream()
                .map(this::toEventDTO)
                .collect(Collectors.toList());
    }

    public List<WorldEventDTO> getUpcomingEvents() {
        Instant now = Instant.now();
        return worldEventRepository.findUpcomingEvents(now).stream()
                .limit(10)
                .map(this::toEventDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "activeEvents", allEntries = true)
    public WorldEvent createEvent(WorldEvent event) {
        if (event.getStartTime() == null) {
            event.setStartTime(Instant.now());
        }
        if (event.getEndTime() == null) {
            event.setEndTime(event.getStartTime().plus(Duration.ofHours(1)));
        }
        return worldEventRepository.save(event);
    }

    @Transactional
    @CacheEvict(value = "activeEvents", allEntries = true)
    public void activateEvent(Long eventId) {
        WorldEvent event = worldEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        event.setActive(true);
        worldEventRepository.save(event);
        log.info("Event activated: {}", event.getEventName());
    }

    @Transactional
    @CacheEvict(value = "activeEvents", allEntries = true)
    public void deactivateEvent(Long eventId) {
        WorldEvent event = worldEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        event.setActive(false);
        worldEventRepository.save(event);
        log.info("Event deactivated: {}", event.getEventName());
    }

    @Cacheable(value = "worldBosses")
    public List<WorldBossDTO> getActiveBosses() {
        return worldBossRepository.findByStatus(WorldBoss.BossStatus.ACTIVE).stream()
                .map(this::toBossDTO)
                .collect(Collectors.toList());
    }

    public List<WorldBossDTO> getRecentBosses() {
        return worldBossRepository.findTop10ByOrderBySpawnTimeDesc().stream()
                .map(this::toBossDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "worldBosses", allEntries = true)
    public WorldBoss spawnBoss(String bossName, Integer level, Long maxHp, String location) {
        WorldBoss boss = WorldBoss.builder()
                .bossName(bossName)
                .bossLevel(level)
                .currentHp(maxHp)
                .maxHp(maxHp)
                .status(WorldBoss.BossStatus.ACTIVE)
                .location(location)
                .spawnTime(Instant.now())
                .build();

        WorldBoss saved = worldBossRepository.save(boss);
        log.info("World boss spawned: {} at {}", bossName, location);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "worldBosses", allEntries = true)
    public WorldBoss updateBossHp(Long bossId, Long damage, String attackerId) {
        WorldBoss boss = worldBossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss not found"));

        if (boss.getStatus() != WorldBoss.BossStatus.ACTIVE) {
            throw new IllegalStateException("Boss is not active");
        }

        Long newHp = Math.max(0, boss.getCurrentHp() - damage);
        boss.setCurrentHp(newHp);

        if (newHp == 0) {
            boss.setStatus(WorldBoss.BossStatus.DEFEATED);
            boss.setDefeatedBy(attackerId);
            boss.setDefeatedTime(Instant.now());
            log.info("World boss defeated: {} by {}", boss.getBossName(), attackerId);
        }

        return worldBossRepository.save(boss);
    }

    @Scheduled(fixedRate = 60000) // Every minute
    @CacheEvict(value = "activeEvents", allEntries = true)
    public void updateEventStatus() {
        Instant now = Instant.now();
        List<WorldEvent> events = worldEventRepository.findAll();

        for (WorldEvent event : events) {
            boolean shouldBeActive = now.isAfter(event.getStartTime()) && now.isBefore(event.getEndTime());
            if (event.getActive() != shouldBeActive) {
                event.setActive(shouldBeActive);
                worldEventRepository.save(event);
                log.info("Event status updated: {} -> {}", event.getEventName(), shouldBeActive);
            }
        }
    }

    private WorldState createDefaultGlobalState() {
        Map<String, Object> defaultState = Map.of(
                "serverStatus", "ONLINE",
                "onlinePlayers", 0,
                "maintenanceMode", false
        );

        try {
            String stateJson = objectMapper.writeValueAsString(defaultState);
            WorldState state = WorldState.builder()
                    .stateKey(GLOBAL_STATE_KEY)
                    .stateData(stateJson)
                    .version(1)
                    .build();
            return worldStateRepository.save(state);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default state");
        }
    }

    private WorldEventDTO toEventDTO(WorldEvent event) {
        Instant now = Instant.now();
        Long remainingSeconds = event.getEndTime().isAfter(now) 
                ? Duration.between(now, event.getEndTime()).getSeconds() 
                : 0L;

        return WorldEventDTO.builder()
                .eventId(event.getEventId())
                .eventName(event.getEventName())
                .eventType(event.getEventType().name())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .active(event.getActive())
                .recurring(event.getRecurring())
                .remainingSeconds(remainingSeconds)
                .build();
    }

    private WorldBossDTO toBossDTO(WorldBoss boss) {
        return WorldBossDTO.builder()
                .bossId(boss.getBossId())
                .bossName(boss.getBossName())
                .bossLevel(boss.getBossLevel())
                .currentHp(boss.getCurrentHp())
                .maxHp(boss.getMaxHp())
                .hpPercentage(boss.getHpPercentage())
                .status(boss.getStatus().name())
                .location(boss.getLocation())
                .spawnTime(boss.getSpawnTime())
                .defeatedBy(boss.getDefeatedBy())
                .build();
    }
}
