package com.SouthMillion.world_service;

import com.SouthMillion.worlds_ervice.service.WorldService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorldService Tests")
class WorldServiceTest {

    @Mock private WorldStateRepository worldStateRepository;
    @Mock private WorldEventRepository  worldEventRepository;
    @Mock private WorldBossRepository   worldBossRepository;
    @Mock private ObjectMapper          objectMapper;

    @InjectMocks private WorldService worldService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static WorldState worldState(int version) {
        WorldState s = new WorldState();
        s.setStateKey("GLOBAL_STATE");
        s.setStateData("{\"serverStatus\":\"ONLINE\"}");
        s.setVersion(version);
        return s;
    }

    private static WorldEvent worldEvent(Long id, boolean active, Instant start, Instant end) {
        WorldEvent e = new WorldEvent();
        e.setEventId(id);
        e.setEventName("Test Event " + id);
        e.setEventType(WorldEvent.EventType.WORLD_BOSS);
        e.setDescription("Test description");
        e.setStartTime(start);
        e.setEndTime(end);
        e.setActive(active);
        e.setRecurring(false);
        return e;
    }

    private static WorldBoss worldBoss(Long id, WorldBoss.BossStatus status, Long currentHp, Long maxHp) {
        return WorldBoss.builder()
                .bossId(id)
                .bossName("Dragon")
                .bossLevel(50)
                .currentHp(currentHp)
                .maxHp(maxHp)
                .status(status)
                .location("Castle")
                .spawnTime(Instant.now())
                .build();
    }

    // =========================================================
    // getGlobalState()
    // =========================================================
    @Nested
    @DisplayName("getGlobalState()")
    class GetGlobalState {

        @Test
        @DisplayName("TC-WORLD-001 [P] State co trong DB – parse JSON va tra ve DTO")
        void getGlobalState_stateFound_returnsDto() throws Exception {
            WorldState ws = worldState(1);
            Map<String, Object> parsed = Map.of("serverStatus", "ONLINE");
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.of(ws));
            given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(parsed);

            WorldStateDTO result = worldService.getGlobalState();

            assertThat(result.getStateKey()).isEqualTo("GLOBAL_STATE");
            assertThat(result.getStateData()).isEqualTo(parsed);
            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getServerTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-WORLD-002 [P] State khong co trong DB – tao default va tra ve")
        void getGlobalState_stateNotFound_createsDefault() throws Exception {
            WorldState defaultState = worldState(1);
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.empty());
            given(objectMapper.writeValueAsString(any())).willReturn("{\"serverStatus\":\"ONLINE\"}");
            given(worldStateRepository.save(any())).willReturn(defaultState);
            Map<String, Object> parsed = Map.of("serverStatus", "ONLINE");
            given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(parsed);

            WorldStateDTO result = worldService.getGlobalState();

            assertThat(result).isNotNull();
            then(worldStateRepository).should().save(any(WorldState.class));
        }

        @Test
        @DisplayName("TC-WORLD-003 [N] JSON parse that bai – tra ve error DTO")
        void getGlobalState_jsonParseFails_returnsErrorDto() throws Exception {
            WorldState ws = worldState(1);
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.of(ws));
            given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .willThrow(new RuntimeException("parse error"));

            WorldStateDTO result = worldService.getGlobalState();

            assertThat(result.getStateData()).containsKey("error");
        }
    }

    // =========================================================
    // updateGlobalState()
    // =========================================================
    @Nested
    @DisplayName("updateGlobalState()")
    class UpdateGlobalState {

        @Test
        @DisplayName("TC-WORLD-004 [P] Cap nhat state hien tai – tang version va luu")
        void updateGlobalState_existingState_incrementsVersionAndSaves() throws Exception {
            WorldState ws = worldState(3);
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.of(ws));
            given(objectMapper.writeValueAsString(any())).willReturn("{\"key\":\"val\"}");
            given(worldStateRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            worldService.updateGlobalState(Map.of("key", "val"));

            then(worldStateRepository).should().save(argThat(s -> s.getVersion() == 4));
        }

        @Test
        @DisplayName("TC-WORLD-005 [P] State chua co trong DB – tao default roi cap nhat")
        void updateGlobalState_noExistingState_createsAndUpdates() throws Exception {
            WorldState savedDefault = worldState(1);
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.empty());
            given(objectMapper.writeValueAsString(any())).willReturn("{\"key\":\"val\"}");
            given(worldStateRepository.save(any())).willReturn(savedDefault);

            WorldState result = worldService.updateGlobalState(Map.of("key", "val"));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-WORLD-006 [N] JSON write that bai – nem RuntimeException")
        void updateGlobalState_jsonWriteFails_throws() throws Exception {
            WorldState ws = worldState(1);
            given(worldStateRepository.findByStateKey("GLOBAL_STATE")).willReturn(Optional.of(ws));
            given(objectMapper.writeValueAsString(any()))
                    .willThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {});

            assertThatThrownBy(() -> worldService.updateGlobalState(Map.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to update world state");
        }
    }

    // =========================================================
    // getActiveEvents()
    // =========================================================
    @Nested
    @DisplayName("getActiveEvents()")
    class GetActiveEvents {

        @Test
        @DisplayName("TC-WORLD-007 [P] Tra ve danh sach su kien dang active")
        void getActiveEvents_returnsActiveDtos() {
            Instant now = Instant.now();
            WorldEvent e = worldEvent(1L, true, now.minusSeconds(60), now.plusSeconds(3600));
            given(worldEventRepository.findActiveEventsAt(any())).willReturn(List.of(e));

            List<WorldEventDTO> result = worldService.getActiveEvents();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo(1L);
            assertThat(result.get(0).getEventName()).isEqualTo("Test Event 1");
        }

        @Test
        @DisplayName("TC-WORLD-008 [P] Khong co su kien active – tra ve list rong")
        void getActiveEvents_noEvents_returnsEmpty() {
            given(worldEventRepository.findActiveEventsAt(any())).willReturn(List.of());

            List<WorldEventDTO> result = worldService.getActiveEvents();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getUpcomingEvents()
    // =========================================================
    @Nested
    @DisplayName("getUpcomingEvents()")
    class GetUpcomingEvents {

        @Test
        @DisplayName("TC-WORLD-009 [P] Tra ve toi da 10 su kien sap toi")
        void getUpcomingEvents_limitsToTen() {
            Instant now = Instant.now();
            List<WorldEvent> events = new ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                events.add(worldEvent((long) i, false,
                        now.plusSeconds((long) i * 3600),
                        now.plusSeconds((long) (i + 1) * 3600)));
            }
            given(worldEventRepository.findUpcomingEvents(any())).willReturn(events);

            List<WorldEventDTO> result = worldService.getUpcomingEvents();

            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("TC-WORLD-010 [P] It hon 10 su kien – tra ve tat ca")
        void getUpcomingEvents_fewEvents_returnsAll() {
            Instant now = Instant.now();
            List<WorldEvent> events = List.of(
                    worldEvent(1L, false, now.plusSeconds(3600), now.plusSeconds(7200)),
                    worldEvent(2L, false, now.plusSeconds(7200), now.plusSeconds(10800))
            );
            given(worldEventRepository.findUpcomingEvents(any())).willReturn(events);

            List<WorldEventDTO> result = worldService.getUpcomingEvents();

            assertThat(result).hasSize(2);
        }
    }

    // =========================================================
    // createEvent()
    // =========================================================
    @Nested
    @DisplayName("createEvent()")
    class CreateEvent {

        @Test
        @DisplayName("TC-WORLD-011 [P] Tao event voi thoi gian co san – luu va tra ve")
        void createEvent_withTimes_savesAndReturns() {
            Instant start = Instant.now();
            Instant end   = start.plusSeconds(3600);
            WorldEvent input = worldEvent(null, false, start, end);
            given(worldEventRepository.save(any())).willReturn(input);

            WorldEvent result = worldService.createEvent(input);

            assertThat(result).isNotNull();
            then(worldEventRepository).should().save(input);
        }

        @Test
        @DisplayName("TC-WORLD-012 [P] Tao event voi startTime null – dat startTime = now")
        void createEvent_nullStartTime_setsDefaultStartTime() {
            WorldEvent input = worldEvent(null, false, null, null);
            given(worldEventRepository.save(any())).willReturn(input);

            worldService.createEvent(input);

            then(worldEventRepository).should().save(argThat(e ->
                    e.getStartTime() != null && e.getEndTime() != null));
        }

        @Test
        @DisplayName("TC-WORLD-013 [P] Tao event voi endTime null – dat endTime = startTime + 1h")
        void createEvent_nullEndTime_setsOneHourDefault() {
            Instant start = Instant.now();
            WorldEvent input = worldEvent(null, false, start, null);
            given(worldEventRepository.save(any())).willReturn(input);

            worldService.createEvent(input);

            then(worldEventRepository).should().save(argThat(e ->
                    e.getEndTime() != null &&
                    !e.getEndTime().isBefore(e.getStartTime())));
        }
    }

    // =========================================================
    // activateEvent()
    // =========================================================
    @Nested
    @DisplayName("activateEvent()")
    class ActivateEvent {

        @Test
        @DisplayName("TC-WORLD-014 [P] Kich hoat event hien co – set active = true va luu")
        void activateEvent_found_setsActiveTrue() {
            Instant now = Instant.now();
            WorldEvent e = worldEvent(10L, false, now.minusSeconds(60), now.plusSeconds(3600));
            given(worldEventRepository.findById(10L)).willReturn(Optional.of(e));

            worldService.activateEvent(10L);

            then(worldEventRepository).should().save(argThat(ev -> Boolean.TRUE.equals(ev.getActive())));
        }

        @Test
        @DisplayName("TC-WORLD-015 [N] Event khong ton tai – nem IllegalArgumentException")
        void activateEvent_notFound_throws() {
            given(worldEventRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> worldService.activateEvent(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found");
        }
    }

    // =========================================================
    // deactivateEvent()
    // =========================================================
    @Nested
    @DisplayName("deactivateEvent()")
    class DeactivateEvent {

        @Test
        @DisplayName("TC-WORLD-016 [P] Tat event – set active = false va luu")
        void deactivateEvent_found_setsActiveFalse() {
            Instant now = Instant.now();
            WorldEvent e = worldEvent(10L, true, now.minusSeconds(60), now.plusSeconds(3600));
            given(worldEventRepository.findById(10L)).willReturn(Optional.of(e));

            worldService.deactivateEvent(10L);

            then(worldEventRepository).should().save(argThat(ev -> Boolean.FALSE.equals(ev.getActive())));
        }

        @Test
        @DisplayName("TC-WORLD-017 [N] Event khong ton tai – nem IllegalArgumentException")
        void deactivateEvent_notFound_throws() {
            given(worldEventRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> worldService.deactivateEvent(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found");
        }
    }

    // =========================================================
    // getActiveBosses()
    // =========================================================
    @Nested
    @DisplayName("getActiveBosses()")
    class GetActiveBosses {

        @Test
        @DisplayName("TC-WORLD-018 [P] Tra ve danh sach boss dang hoat dong")
        void getActiveBosses_returnsDtos() {
            WorldBoss b = worldBoss(1L, WorldBoss.BossStatus.ACTIVE, 80000L, 100000L);
            given(worldBossRepository.findByStatus(WorldBoss.BossStatus.ACTIVE)).willReturn(List.of(b));

            List<WorldBossDTO> result = worldService.getActiveBosses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBossId()).isEqualTo(1L);
            assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("TC-WORLD-019 [P] Khong co boss active – tra ve list rong")
        void getActiveBosses_noBosses_returnsEmpty() {
            given(worldBossRepository.findByStatus(WorldBoss.BossStatus.ACTIVE)).willReturn(List.of());

            List<WorldBossDTO> result = worldService.getActiveBosses();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getRecentBosses()
    // =========================================================
    @Nested
    @DisplayName("getRecentBosses()")
    class GetRecentBosses {

        @Test
        @DisplayName("TC-WORLD-020 [P] Tra ve top 10 boss gan nhat")
        void getRecentBosses_returnsDtos() {
            WorldBoss b1 = worldBoss(1L, WorldBoss.BossStatus.DEFEATED, 0L, 10000L);
            WorldBoss b2 = worldBoss(2L, WorldBoss.BossStatus.ACTIVE,   5000L, 10000L);
            given(worldBossRepository.findTop10ByOrderBySpawnTimeDesc()).willReturn(List.of(b1, b2));

            List<WorldBossDTO> result = worldService.getRecentBosses();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBossId()).isEqualTo(1L);
        }
    }

    // =========================================================
    // spawnBoss()
    // =========================================================
    @Nested
    @DisplayName("spawnBoss()")
    class SpawnBoss {

        @Test
        @DisplayName("TC-WORLD-021 [P] Tao boss moi – luu va tra ve boss ACTIVE")
        void spawnBoss_createsAndSavesBoss() {
            WorldBoss saved = worldBoss(1L, WorldBoss.BossStatus.ACTIVE, 50000L, 50000L);
            given(worldBossRepository.save(any())).willReturn(saved);

            WorldBoss result = worldService.spawnBoss("Dragon", 50, 50000L, "Castle");

            assertThat(result.getBossName()).isEqualTo("Dragon");
            assertThat(result.getStatus()).isEqualTo(WorldBoss.BossStatus.ACTIVE);
            then(worldBossRepository).should().save(argThat(b ->
                    "Dragon".equals(b.getBossName()) &&
                    b.getBossLevel().equals(50) &&
                    b.getCurrentHp().equals(50000L) &&
                    b.getMaxHp().equals(50000L) &&
                    b.getStatus() == WorldBoss.BossStatus.ACTIVE));
        }
    }

    // =========================================================
    // updateBossHp()
    // =========================================================
    @Nested
    @DisplayName("updateBossHp()")
    class UpdateBossHp {

        @Test
        @DisplayName("TC-WORLD-022 [P] Giam HP boss – boss van song")
        void updateBossHp_partialDamage_bossStillAlive() {
            WorldBoss b = worldBoss(1L, WorldBoss.BossStatus.ACTIVE, 10000L, 10000L);
            given(worldBossRepository.findById(1L)).willReturn(Optional.of(b));
            given(worldBossRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            WorldBoss result = worldService.updateBossHp(1L, 3000L, "player1");

            assertThat(result.getCurrentHp()).isEqualTo(7000L);
            assertThat(result.getStatus()).isEqualTo(WorldBoss.BossStatus.ACTIVE);
            assertThat(result.getDefeatedBy()).isNull();
        }

        @Test
        @DisplayName("TC-WORLD-023 [P] HP boss giam ve 0 – boss chuyen sang DEFEATED")
        void updateBossHp_lethalDamage_bossDefeated() {
            WorldBoss b = worldBoss(1L, WorldBoss.BossStatus.ACTIVE, 5000L, 10000L);
            given(worldBossRepository.findById(1L)).willReturn(Optional.of(b));
            given(worldBossRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            WorldBoss result = worldService.updateBossHp(1L, 5000L, "hero123");

            assertThat(result.getCurrentHp()).isEqualTo(0L);
            assertThat(result.getStatus()).isEqualTo(WorldBoss.BossStatus.DEFEATED);
            assertThat(result.getDefeatedBy()).isEqualTo("hero123");
            assertThat(result.getDefeatedTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-WORLD-024 [P] Sat thuong lon hon HP – HP giam xuong 0 (khong am)")
        void updateBossHp_overkillDamage_hpFloorAtZero() {
            WorldBoss b = worldBoss(1L, WorldBoss.BossStatus.ACTIVE, 1000L, 10000L);
            given(worldBossRepository.findById(1L)).willReturn(Optional.of(b));
            given(worldBossRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            WorldBoss result = worldService.updateBossHp(1L, 99999L, "overkill");

            assertThat(result.getCurrentHp()).isEqualTo(0L);
            assertThat(result.getStatus()).isEqualTo(WorldBoss.BossStatus.DEFEATED);
        }

        @Test
        @DisplayName("TC-WORLD-025 [N] Boss khong ton tai – nem IllegalArgumentException")
        void updateBossHp_bossNotFound_throws() {
            given(worldBossRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> worldService.updateBossHp(99L, 1000L, "player1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Boss not found");
        }

        @Test
        @DisplayName("TC-WORLD-026 [N] Boss khong hoat dong – nem IllegalStateException")
        void updateBossHp_bossNotActive_throws() {
            WorldBoss b = worldBoss(1L, WorldBoss.BossStatus.DEFEATED, 0L, 10000L);
            given(worldBossRepository.findById(1L)).willReturn(Optional.of(b));

            assertThatThrownBy(() -> worldService.updateBossHp(1L, 1000L, "player1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }
    }
}
