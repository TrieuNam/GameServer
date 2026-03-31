package com.SouthMillion.leaderboard_service.service;

import com.SouthMillion.leaderboard_service.dto.LeaderboardDTO;
import com.SouthMillion.leaderboard_service.entity.RankingEntry;
import com.SouthMillion.leaderboard_service.repository.RankingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService Tests")
class LeaderboardServiceTest {

    @Mock private RankingRepository                 rankingRepository;
    @Mock private RedisTemplate<String, Object>     redisTemplate;
    @Mock private ValueOperations<String, Object>   valueOps;

    @InjectMocks private LeaderboardService leaderboardService;

    private static RankingEntry entry(String roleId, int type, long score) {
        return RankingEntry.builder()
                .rankingType(type)
                .roleId(roleId)
                .roleName("Player_" + roleId)
                .roleLevel(50)
                .score(score)
                .build();
    }

    // =========================================================
    // updateScore
    // =========================================================
    @Nested
    @DisplayName("updateScore()")
    class UpdateScore {

        @Test
        @DisplayName("TC-LB-001 [P] Cap nhat diem moi – tao entry moi va xoa cache")
        void updateScore_noExisting_createsEntry() {
            given(rankingRepository.findByRankingTypeAndRoleId(1, "role1")).willReturn(Optional.empty());
            given(rankingRepository.save(any(RankingEntry.class))).willAnswer(inv -> inv.getArgument(0));
            given(rankingRepository.findTopByType(eq(1), any(Pageable.class))).willReturn(List.of());
            given(redisTemplate.delete(anyString())).willReturn(true);

            LeaderboardDTO.UpdateScoreRequest req = LeaderboardDTO.UpdateScoreRequest.builder()
                    .rankingType(1).roleId("role1").roleName("Player1")
                    .roleLevel(60).score(5000L).build();

            LeaderboardDTO.Response<LeaderboardDTO.RankingInfo> result = leaderboardService.updateScore(req);

            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getData().getRoleId()).isEqualTo("role1");
            then(rankingRepository).should(atLeastOnce()).save(any(RankingEntry.class));
            then(redisTemplate).should().delete("leaderboard:1");
        }

        @Test
        @DisplayName("TC-LB-002 [P] Cap nhat diem hien tai – sua doi entry cu")
        void updateScore_existing_updatesEntry() {
            RankingEntry existing = entry("role2", 1, 3000L);
            given(rankingRepository.findByRankingTypeAndRoleId(1, "role2")).willReturn(Optional.of(existing));
            given(rankingRepository.save(any(RankingEntry.class))).willAnswer(inv -> inv.getArgument(0));
            given(rankingRepository.findTopByType(eq(1), any(Pageable.class))).willReturn(List.of(existing));
            given(redisTemplate.delete(anyString())).willReturn(true);

            LeaderboardDTO.UpdateScoreRequest req = LeaderboardDTO.UpdateScoreRequest.builder()
                    .rankingType(1).roleId("role2").roleName("Player2")
                    .roleLevel(70).score(8000L).build();

            LeaderboardDTO.Response<LeaderboardDTO.RankingInfo> result = leaderboardService.updateScore(req);

            assertThat(result.getCode()).isEqualTo(0);
            assertThat(existing.getScore()).isEqualTo(8000L);
        }
    }

    // =========================================================
    // getLeaderboard
    // =========================================================
    @Nested
    @DisplayName("getLeaderboard()")
    class GetLeaderboard {

        @Test
        @DisplayName("TC-LB-003 [P] Cache hit – tra ve du lieu tu cache")
        void getLeaderboard_cacheHit_returnsCachedData() {
            LeaderboardDTO.RankingInfo info = LeaderboardDTO.RankingInfo.builder()
                    .rank(1).roleId("roleA").score(9999L).build();
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("leaderboard:1")).willReturn(List.of(info));

            LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> result =
                    leaderboardService.getLeaderboard(1, null);

            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getData().getRankings()).hasSize(1);
            assertThat(result.getData().getRankings().get(0).getRoleId()).isEqualTo("roleA");
            then(rankingRepository).should(never()).findTopByType(any(), any());
        }

        @Test
        @DisplayName("TC-LB-004 [P] Cache miss – lay du lieu tu DB va luu cache")
        void getLeaderboard_cacheMiss_fetchesFromDb() {
            RankingEntry e = entry("roleB", 2, 7000L);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("leaderboard:2")).willReturn(null); // cache miss
            given(rankingRepository.findTopByType(eq(2), any(Pageable.class))).willReturn(List.of(e));

            LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> result =
                    leaderboardService.getLeaderboard(2, null);

            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getData().getRankings()).hasSize(1);
            then(valueOps).should().set(eq("leaderboard:2"), any(), anyLong(), any());
        }

        @Test
        @DisplayName("TC-LB-005 [P] Cache miss co currentRoleId – bao gom hang hien tai cua nguoi choi")
        void getLeaderboard_cacheMissWithRoleId_includesMyRank() {
            RankingEntry e = entry("roleC", 3, 5000L);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("leaderboard:3")).willReturn(null);
            given(rankingRepository.findTopByType(eq(3), any(Pageable.class))).willReturn(List.of(e));
            given(rankingRepository.findPlayerRank(3, "roleC")).willReturn(Optional.of(e));
            given(rankingRepository.getRankByScore(3, 5000L)).willReturn(0);

            LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> result =
                    leaderboardService.getLeaderboard(3, "roleC");

            assertThat(result.getData().getMyRank()).isNotNull();
            assertThat(result.getData().getMyRank().getRoleId()).isEqualTo("roleC");
        }

        @Test
        @DisplayName("TC-LB-006 [P] Cache miss co currentRoleId nhung nguoi choi chua co hang")
        void getLeaderboard_roleIdNotRanked_myRankIsNull() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("leaderboard:1")).willReturn(null);
            given(rankingRepository.findTopByType(eq(1), any(Pageable.class))).willReturn(List.of());
            given(rankingRepository.findPlayerRank(1, "newRole")).willReturn(Optional.empty());

            LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> result =
                    leaderboardService.getLeaderboard(1, "newRole");

            assertThat(result.getData().getMyRank()).isNull();
        }
    }

    // =========================================================
    // refreshAllLeaderboards
    // =========================================================
    @Nested
    @DisplayName("refreshAllLeaderboards()")
    class RefreshAllLeaderboards {

        @Test
        @DisplayName("TC-LB-007 [P] Lam moi tat ca 8 loai bang xep hang")
        void refreshAllLeaderboards_callsUpdateAndClearForAllTypes() {
            given(rankingRepository.findTopByType(anyInt(), any(Pageable.class))).willReturn(List.of());
            given(redisTemplate.delete(anyString())).willReturn(true);

            leaderboardService.refreshAllLeaderboards();

            // Should call delete for all 8 ranking types
            then(redisTemplate).should(times(8)).delete(anyString());
        }
    }
}
