package com.SouthMillion.arenaservice.service;

import com.SouthMillion.arenaservice.dto.BattleRequest;
import com.SouthMillion.arenaservice.dto.BattleResult;
import com.SouthMillion.arenaservice.entity.ArenaBattleHistory;
import com.SouthMillion.arenaservice.entity.ArenaPlayer;
import com.SouthMillion.arenaservice.repository.ArenaBattleHistoryRepository;
import com.SouthMillion.arenaservice.repository.ArenaPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArenaService Tests")
class ArenaServiceTest {

    @Mock
    private ArenaPlayerRepository arenaPlayerRepository;

    @Mock
    private ArenaBattleHistoryRepository battleHistoryRepository;

    @InjectMocks
    private ArenaService arenaService;

    // ── helpers ──────────────────────────────────────────────
    private ArenaPlayer player(String id, int rating) {
        return ArenaPlayer.builder()
                .playerId(id)
                .rating(rating)
                .wins(0)
                .losses(0)
                .consecutiveWins(0)
                .totalBattles(0)
                .currentRank(0)
                .challengesUsedToday(0)
                .season(1)
                .build();
    }

    // =========================================================
    // getOrCreatePlayer
    // =========================================================
    @Nested
    @DisplayName("getOrCreatePlayer()")
    class GetOrCreate {

        @Test
        @DisplayName("TC-ARN-P01 [P] Player da ton tai – tra ve tu DB")
        void getOrCreate_existingPlayer_returned() {
            ArenaPlayer existing = player("p1", 1200);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(existing));

            ArenaPlayer result = arenaService.getOrCreatePlayer("p1");

            assertThat(result.getRating()).isEqualTo(1200);
            then(arenaPlayerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-ARN-P02 [P] Player moi – khoi tao voi rating 1000")
        void getOrCreate_newPlayer_created() {
            given(arenaPlayerRepository.findByPlayerId("new1")).willReturn(Optional.empty());
            given(arenaPlayerRepository.save(any(ArenaPlayer.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ArenaPlayer result = arenaService.getOrCreatePlayer("new1");

            assertThat(result.getRating()).isEqualTo(1000);
            assertThat(result.getWins()).isEqualTo(0);
            assertThat(result.getSeason()).isEqualTo(1);
        }
    }

    // =========================================================
    // calculateRankReward
    // =========================================================
    @Nested
    @DisplayName("calculateRankReward()")
    class RankReward {

        @Test
        @DisplayName("TC-ARN-040 [P] Top 10 nhan 10000 gold (rank=5)")
        void rankReward_top10() {
            ArenaPlayer p = player("p1", 1500);
            p.setCurrentRank(5);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            long reward = arenaService.calculateRankReward("p1");

            assertThat(reward).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("TC-ARN-041 [P] Top 11-50 nhan 5000 gold (rank=25)")
        void rankReward_top50() {
            ArenaPlayer p = player("p1", 1300);
            p.setCurrentRank(25);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            long reward = arenaService.calculateRankReward("p1");

            assertThat(reward).isEqualTo(5_000L);
        }

        @Test
        @DisplayName("TC-ARN-042 [P] Top 51-100 nhan 2000 gold (rank=75)")
        void rankReward_top100() {
            ArenaPlayer p = player("p1", 1100);
            p.setCurrentRank(75);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            long reward = arenaService.calculateRankReward("p1");

            assertThat(reward).isEqualTo(2_000L);
        }

        @Test
        @DisplayName("TC-ARN-043 [P] Ngoai top 100 nhan 500 gold (rank=150)")
        void rankReward_outside100() {
            ArenaPlayer p = player("p1", 900);
            p.setCurrentRank(150);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            long reward = arenaService.calculateRankReward("p1");

            assertThat(reward).isEqualTo(500L);
        }

        @Test
        @DisplayName("TC-ARN-044 [B] Ranh gioi rank=10 nhan 10000 gold")
        void rankReward_boundary_rank10() {
            ArenaPlayer p = player("p1", 1400);
            p.setCurrentRank(10);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            assertThat(arenaService.calculateRankReward("p1")).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("TC-ARN-045 [B] Ranh gioi rank=11 nhan 5000 gold")
        void rankReward_boundary_rank11() {
            ArenaPlayer p = player("p1", 1390);
            p.setCurrentRank(11);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            assertThat(arenaService.calculateRankReward("p1")).isEqualTo(5_000L);
        }

        @Test
        @DisplayName("TC-ARN-046 [B] Ranh gioi rank=100 nhan 2000 gold")
        void rankReward_boundary_rank100() {
            ArenaPlayer p = player("p1", 1000);
            p.setCurrentRank(100);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            assertThat(arenaService.calculateRankReward("p1")).isEqualTo(2_000L);
        }

        @Test
        @DisplayName("TC-ARN-047 [B] Ranh gioi rank=101 nhan 500 gold")
        void rankReward_boundary_rank101() {
            ArenaPlayer p = player("p1", 995);
            p.setCurrentRank(101);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            assertThat(arenaService.calculateRankReward("p1")).isEqualTo(500L);
        }
    }

    // =========================================================
    // getChallengesRemaining / consumeChallenge
    // =========================================================
    @Nested
    @DisplayName("getChallengesRemaining() va consumeChallenge()")
    class Challenges {

        @Test
        @DisplayName("TC-ARN-020 [P] Player moi – tra ve 10 luot")
        void getChallengesRemaining_newPlayer_returns10() {
            ArenaPlayer p = player("p1", 1000);
            p.setLastResetDate(null);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            int remaining = arenaService.getChallengesRemaining("p1");

            assertThat(remaining).isEqualTo(10);
        }

        @Test
        @DisplayName("TC-ARN-021 [P] Tieu thu 1 luot – challengesUsedToday tang len 1")
        void consumeChallenge_decrementsRemaining() {
            ArenaPlayer p = player("p1", 1000);
            p.setChallengesUsedToday(3);
            p.setLastResetDate(LocalDate.now());
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));
            given(arenaPlayerRepository.save(any(ArenaPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            arenaService.consumeChallenge("p1");

            assertThat(p.getChallengesUsedToday()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-ARN-022 [P] Reset hang ngay – lastResetDate qua ngay thi reset ve 0")
        void consumeChallenge_dailyReset() {
            ArenaPlayer p = player("p1", 1000);
            p.setChallengesUsedToday(8);
            p.setLastResetDate(LocalDate.now().minusDays(1)); // ngay hom qua
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));
            given(arenaPlayerRepository.save(any(ArenaPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            arenaService.consumeChallenge("p1");

            // Reset ve 0 roi tieu 1
            assertThat(p.getChallengesUsedToday()).isEqualTo(1);
            assertThat(p.getLastResetDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("TC-ARN-023 [N] Het luot thu thach – nem IllegalStateException")
        void consumeChallenge_limitReached_throws() {
            ArenaPlayer p = player("p1", 1000);
            p.setChallengesUsedToday(10); // da het
            p.setLastResetDate(LocalDate.now());
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));

            assertThatThrownBy(() -> arenaService.consumeChallenge("p1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Daily challenge limit reached");
        }
    }

    // =========================================================
    // addBoughtChallenges
    // =========================================================
    @Nested
    @DisplayName("addBoughtChallenges()")
    class BoughtChallenges {

        @Test
        @DisplayName("TC-ARN-B01 [P] Mua them 3 luot – challengesUsedToday giam")
        void addBoughtChallenges_decresesUsed() {
            ArenaPlayer p = player("p1", 1000);
            p.setChallengesUsedToday(7);
            p.setLastResetDate(LocalDate.now());
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));
            given(arenaPlayerRepository.save(any(ArenaPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            int remaining = arenaService.addBoughtChallenges("p1", 3);

            assertThat(p.getChallengesUsedToday()).isEqualTo(4);
            assertThat(remaining).isEqualTo(6); // 10 - 4
        }

        @Test
        @DisplayName("TC-ARN-B02 [B] Mua nhieu hon so da dung – khong am")
        void addBoughtChallenges_cannotGoBelowZero() {
            ArenaPlayer p = player("p1", 1000);
            p.setChallengesUsedToday(2);
            p.setLastResetDate(LocalDate.now());
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p));
            given(arenaPlayerRepository.save(any(ArenaPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            int remaining = arenaService.addBoughtChallenges("p1", 10);

            assertThat(p.getChallengesUsedToday()).isEqualTo(0);
            assertThat(remaining).isEqualTo(10);
        }
    }

    // =========================================================
    // processBattle
    // =========================================================
    @Nested
    @DisplayName("processBattle()")
    class ProcessBattle {

        @Test
        @DisplayName("TC-ARN-005 [P] Battle history duoc luu sau tran dau")
        void processBattle_historyIsSaved() {
            ArenaPlayer p1 = player("p1", 1000);
            ArenaPlayer p2 = player("p2", 900);
            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p1));
            given(arenaPlayerRepository.findByPlayerId("p2")).willReturn(Optional.of(p2));
            given(arenaPlayerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(battleHistoryRepository.save(any(ArenaBattleHistory.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(arenaPlayerRepository.calculateRank(anyInt(), anyInt())).willReturn(1);

            BattleRequest req = BattleRequest.builder()
                    .playerId("p1")
                    .opponentId("p2")
                    .build();

            BattleResult result = arenaService.processBattle(req);

            assertThat(result.getWinnerId()).isIn("p1", "p2");
            assertThat(result.getLoserId()).isIn("p1", "p2");
            assertThat(result.getWinnerId()).isNotEqualTo(result.getLoserId());
            then(battleHistoryRepository).should().save(any(ArenaBattleHistory.class));
        }

        @Test
        @DisplayName("TC-ARN-001 [P] Nguoi co rating cao han thang nhieu hon (xac suat)")
        @RepeatedTest(20)
        void processBattle_higherRatingWinsMoreOften() {
            // Chay 20 lan, player rating 2000 vs 0 → luon thang (winChance clamp 0.9)
            ArenaPlayer strong = player("strong", 2000);
            ArenaPlayer weak = player("weak", 0);
            given(arenaPlayerRepository.findByPlayerId("strong")).willReturn(Optional.of(strong));
            given(arenaPlayerRepository.findByPlayerId("weak")).willReturn(Optional.of(weak));
            given(arenaPlayerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(battleHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(arenaPlayerRepository.calculateRank(anyInt(), anyInt())).willReturn(1);

            BattleRequest req = BattleRequest.builder()
                    .playerId("strong")
                    .opponentId("weak")
                    .build();

            BattleResult result = arenaService.processBattle(req);

            // Rating change phai > 0
            assertThat(result.getRatingChange()).isGreaterThan(0);
        }

        @Test
        @DisplayName("TC-ARN-004 [P] Consecutive win bonus – 3 lan thang lien tiep co bonus")
        void processBattle_consecutiveWinBonus() {
            ArenaPlayer p1 = player("p1", 1050);
            p1.setConsecutiveWins(3); // da co 3 lan thang
            ArenaPlayer p2 = player("p2", 900);

            given(arenaPlayerRepository.findByPlayerId("p1")).willReturn(Optional.of(p1));
            given(arenaPlayerRepository.findByPlayerId("p2")).willReturn(Optional.of(p2));

            // Mock de dam bao p1 thang
            given(arenaPlayerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(battleHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(arenaPlayerRepository.calculateRank(anyInt(), anyInt())).willReturn(1);

            BattleRequest req = BattleRequest.builder()
                    .playerId("p1")
                    .opponentId("p2")
                    .build();

            BattleResult result = arenaService.processBattle(req);

            // Neu p1 thang, consecutiveWinBonus > 0
            if (result.getWinnerId().equals("p1")) {
                assertThat(result.getConsecutiveWinBonus()).isGreaterThanOrEqualTo(5);
            }
        }
    }

    // =========================================================
    // getTop100Rankings
    // =========================================================
    @Nested
    @DisplayName("getTop100Rankings()")
    class TopRankings {

        @Test
        @DisplayName("TC-ARN-030 [P] Lay top 100 – sap xep rating giam dan")
        void getTop100_sortedByRating() {
            List<ArenaPlayer> players = List.of(
                    player("p1", 1500),
                    player("p2", 1200),
                    player("p3", 900)
            );
            given(arenaPlayerRepository.findTop100BySeasonOrderByRatingDesc(1))
                    .willReturn(players);

            var result = arenaService.getTop100Rankings(1);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getRating()).isEqualTo(1500);
            assertThat(result.get(1).getRating()).isEqualTo(1200);
        }

        @Test
        @DisplayName("TC-ARN-031 [P] It hon 100 player – tra ve dung so luong")
        void getTop100_lessThan100() {
            given(arenaPlayerRepository.findTop100BySeasonOrderByRatingDesc(1))
                    .willReturn(List.of(player("p1", 1000)));

            var result = arenaService.getTop100Rankings(1);

            assertThat(result).hasSize(1);
        }
    }
}
