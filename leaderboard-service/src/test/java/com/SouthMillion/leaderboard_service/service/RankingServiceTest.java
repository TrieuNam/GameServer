package com.SouthMillion.leaderboard_service.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService Tests")
class RankingServiceTest {

    @SuppressWarnings("unchecked")
    @Mock private RedisTemplate<String, String> redisTemplate;

    @SuppressWarnings("unchecked")
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks private RankingService rankingService;

    private static final String ROLE_ID     = "role-001";
    private static final String PLAYER_NAME = "PlayerOne";
    private static final String ARENA_KEY   = "ranking:arena:global";
    private static final String SEASON_ID   = "season-2026";
    private static final long   TTL_DAYS    = 90L;

    @BeforeEach
    void setUp() {
        // Most tests need opsForZSet stubbed; set it up globally.
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    }

    // =========================================================
    // updateArenaRanking()
    // =========================================================
    @Nested
    @DisplayName("updateArenaRanking()")
    class UpdateArenaRanking {

        @Test
        @DisplayName("TC-RNK-001 [P] Cap nhat xep hang arena – add member vao Redis ZSet va dat TTL")
        void updateArenaRanking_valid_addsToZSetAndSetsExpiry() {
            String member = ROLE_ID + ":" + PLAYER_NAME;
            given(zSetOps.add(ARENA_KEY, member, 1500)).willReturn(true);
            given(zSetOps.reverseRank(ARENA_KEY, member)).willReturn(0L);

            rankingService.updateArenaRanking(ROLE_ID, PLAYER_NAME, 1500, true);

            then(zSetOps).should().add(ARENA_KEY, member, 1500);
            then(redisTemplate).should().expire(ARENA_KEY, TTL_DAYS, TimeUnit.DAYS);
        }

        @Test
        @DisplayName("TC-RNK-002 [N] Redis bi loi – exception duoc nuot vao, khong lan ra ngoai")
        void updateArenaRanking_redisException_swallowedException() {
            String member = ROLE_ID + ":" + PLAYER_NAME;
            given(zSetOps.add(any(), any(), anyDouble()))
                    .willThrow(new RuntimeException("Redis connection refused"));

            assertThatCode(() ->
                    rankingService.updateArenaRanking(ROLE_ID, PLAYER_NAME, 1500, true))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================
    // updateSeasonRanking()
    // =========================================================
    @Nested
    @DisplayName("updateSeasonRanking()")
    class UpdateSeasonRanking {

        @Test
        @DisplayName("TC-RNK-003 [P] Cap nhat xep hang mua giai – add vao key mua va dat TTL")
        void updateSeasonRanking_valid_addsToSeasonKeyWithTtl() {
            String seasonKey = "ranking:arena:season:" + SEASON_ID;
            given(zSetOps.add(seasonKey, ROLE_ID, 2000)).willReturn(true);
            given(zSetOps.reverseRank(seasonKey, ROLE_ID)).willReturn(2L);

            rankingService.updateSeasonRanking(ROLE_ID, SEASON_ID, 2000);

            then(zSetOps).should().add(seasonKey, ROLE_ID, 2000);
            then(redisTemplate).should().expire(seasonKey, TTL_DAYS, TimeUnit.DAYS);
        }

        @Test
        @DisplayName("TC-RNK-004 [N] Redis loi – exception duoc nuot vao")
        void updateSeasonRanking_redisException_swallowed() {
            given(zSetOps.add(any(), any(), anyDouble()))
                    .willThrow(new RuntimeException("Redis timeout"));

            assertThatCode(() ->
                    rankingService.updateSeasonRanking(ROLE_ID, SEASON_ID, 2000))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================
    // updateTrialRanking()
    // =========================================================
    @Nested
    @DisplayName("updateTrialRanking()")
    class UpdateTrialRanking {

        @Test
        @DisplayName("TC-RNK-005 [P] Cap nhat xep hang thu thach – compositeScore = stars*1000000 + score - time/1000")
        void updateTrialRanking_valid_compositeScoreCalculatedCorrectly() {
            int trialId = 5;
            long score = 5000L;
            int stars = 3;
            long completionTime = 30000L;  // 30 seconds in ms
            // compositeScore = 3*1000000 + 5000 - 30000/1000 = 3000000 + 5000 - 30 = 3004970.0
            double expectedScore = 3004970.0;
            String trialKey = "ranking:trial:" + trialId;
            String expectedMember = ROLE_ID + ":" + stars + ":" + score + ":" + completionTime;

            given(zSetOps.add(eq(trialKey), eq(expectedMember), eq(expectedScore))).willReturn(true);
            given(zSetOps.reverseRank(trialKey, expectedMember)).willReturn(0L);

            rankingService.updateTrialRanking(ROLE_ID, trialId, score, stars, completionTime);

            then(zSetOps).should().add(trialKey, expectedMember, expectedScore);
            then(redisTemplate).should().expire(trialKey, TTL_DAYS, TimeUnit.DAYS);
        }

        @Test
        @DisplayName("TC-RNK-006 [N] Redis loi khi cap nhat thu thach – exception duoc nuot vao")
        void updateTrialRanking_redisException_swallowed() {
            given(zSetOps.add(any(), any(), anyDouble()))
                    .willThrow(new RuntimeException("Connection lost"));

            assertThatCode(() ->
                    rankingService.updateTrialRanking(ROLE_ID, 1, 1000L, 2, 15000L))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================
    // updateGlobalTrialRanking()
    // =========================================================
    @Nested
    @DisplayName("updateGlobalTrialRanking()")
    class UpdateGlobalTrialRanking {

        @Test
        @DisplayName("TC-RNK-007 [P] Cap nhat tong diem thu thach toan cau – incrementScore duoc goi")
        void updateGlobalTrialRanking_valid_incrementsScore() {
            String globalKey = "ranking:trial:global";
            long totalScore = 12000L;
            given(zSetOps.incrementScore(globalKey, ROLE_ID, totalScore)).willReturn(12000.0);
            given(zSetOps.score(globalKey, ROLE_ID)).willReturn(12000.0);
            given(zSetOps.reverseRank(globalKey, ROLE_ID)).willReturn(1L);

            rankingService.updateGlobalTrialRanking(ROLE_ID, totalScore);

            then(zSetOps).should().incrementScore(globalKey, ROLE_ID, totalScore);
            then(redisTemplate).should().expire(globalKey, TTL_DAYS, TimeUnit.DAYS);
        }

        @Test
        @DisplayName("TC-RNK-008 [N] Redis loi khi cap nhat tong diem – exception duoc nuot vao")
        void updateGlobalTrialRanking_redisException_swallowed() {
            given(zSetOps.incrementScore(any(), any(), anyDouble()))
                    .willThrow(new RuntimeException("Redis unavailable"));

            assertThatCode(() ->
                    rankingService.updateGlobalTrialRanking(ROLE_ID, 5000L))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================
    // getTopArenaPlayers()
    // =========================================================
    @Nested
    @DisplayName("getTopArenaPlayers()")
    class GetTopArenaPlayers {

        @Test
        @DisplayName("TC-RNK-009 [P] Lay top 10 nguoi choi arena – tra ve set tu ZSet theo thu tu giam dan")
        void getTopArenaPlayers_returnsFromRedisZSet() {
            @SuppressWarnings("unchecked")
            ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
            Set<ZSetOperations.TypedTuple<String>> expected = Set.of(tuple);
            given(zSetOps.reverseRangeWithScores(ARENA_KEY, 0, 9)).willReturn(expected);

            Set<ZSetOperations.TypedTuple<String>> result = rankingService.getTopArenaPlayers(10);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("TC-RNK-010 [P] Top N = 1 – lay dung vi tri dau tien")
        void getTopArenaPlayers_topOne_queriesCorrectRange() {
            given(zSetOps.reverseRangeWithScores(ARENA_KEY, 0, 0)).willReturn(Set.of());

            rankingService.getTopArenaPlayers(1);

            then(zSetOps).should().reverseRangeWithScores(ARENA_KEY, 0, 0);
        }
    }

    // =========================================================
    // getArenaRank()
    // =========================================================
    @Nested
    @DisplayName("getArenaRank()")
    class GetArenaRank {

        @Test
        @DisplayName("TC-RNK-011 [P] Nguoi choi co trong ranking – tra ve rank 1-based")
        void getArenaRank_playerExists_returns1BasedRank() {
            String member = ROLE_ID + ":" + PLAYER_NAME;
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(Set.of(member, "other:Player2"));
            given(zSetOps.reverseRank(ARENA_KEY, member)).willReturn(0L); // 0-based

            Long rank = rankingService.getArenaRank(ROLE_ID);

            assertThat(rank).isEqualTo(1L); // 0 + 1 = 1
        }

        @Test
        @DisplayName("TC-RNK-012 [N] Nguoi choi khong co trong ranking – tra ve null")
        void getArenaRank_playerNotFound_returnsNull() {
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(Set.of("other:PlayerB"));

            Long rank = rankingService.getArenaRank(ROLE_ID);

            assertThat(rank).isNull();
        }

        @Test
        @DisplayName("TC-RNK-013 [N] Ranking rong – tra ve null")
        void getArenaRank_emptyRanking_returnsNull() {
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(Set.of());

            Long rank = rankingService.getArenaRank(ROLE_ID);

            assertThat(rank).isNull();
        }

        @Test
        @DisplayName("TC-RNK-014 [N] ZSet tra ve null – tra ve null an toan")
        void getArenaRank_nullMemberSet_returnsNull() {
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(null);

            Long rank = rankingService.getArenaRank(ROLE_ID);

            assertThat(rank).isNull();
        }
    }

    // =========================================================
    // getArenaRating()
    // =========================================================
    @Nested
    @DisplayName("getArenaRating()")
    class GetArenaRating {

        @Test
        @DisplayName("TC-RNK-015 [P] Nguoi choi co trong ranking – tra ve diem so chinh xac")
        void getArenaRating_playerExists_returnsRating() {
            String member = ROLE_ID + ":" + PLAYER_NAME;
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(Set.of(member));
            given(zSetOps.score(ARENA_KEY, member)).willReturn(1800.0);

            Double rating = rankingService.getArenaRating(ROLE_ID);

            assertThat(rating).isEqualTo(1800.0);
        }

        @Test
        @DisplayName("TC-RNK-016 [N] Nguoi choi khong co – tra ve null")
        void getArenaRating_playerNotFound_returnsNull() {
            given(zSetOps.range(ARENA_KEY, 0, -1)).willReturn(Set.of("other:Player"));

            Double rating = rankingService.getArenaRating(ROLE_ID);

            assertThat(rating).isNull();
        }
    }

    // =========================================================
    // getTopTrialPlayers()
    // =========================================================
    @Nested
    @DisplayName("getTopTrialPlayers()")
    class GetTopTrialPlayers {

        @Test
        @DisplayName("TC-RNK-017 [P] Lay top 5 nguoi choi thu thach id=3 – dung key thu thach cu the")
        void getTopTrialPlayers_returnsFromTrialSpecificKey() {
            int trialId = 3;
            String trialKey = "ranking:trial:" + trialId;
            @SuppressWarnings("unchecked")
            ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
            Set<ZSetOperations.TypedTuple<String>> expected = Set.of(tuple);
            given(zSetOps.reverseRangeWithScores(trialKey, 0, 4)).willReturn(expected);

            Set<ZSetOperations.TypedTuple<String>> result = rankingService.getTopTrialPlayers(trialId, 5);

            assertThat(result).isEqualTo(expected);
            then(zSetOps).should().reverseRangeWithScores(trialKey, 0, 4);
        }
    }
}
