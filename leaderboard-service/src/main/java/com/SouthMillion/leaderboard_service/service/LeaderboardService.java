package com.SouthMillion.leaderboard_service.service;

import com.SouthMillion.leaderboard_service.dto.LeaderboardDTO;
import com.SouthMillion.leaderboard_service.entity.RankingEntry;
import com.SouthMillion.leaderboard_service.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Leaderboard Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RankingRepository rankingRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int TOP_LIMIT = 100;
    private static final Map<Integer, String> RANKING_NAMES = new HashMap<>();

    static {
        RANKING_NAMES.put(1, "Power Ranking");
        RANKING_NAMES.put(2, "Level Ranking");
        RANKING_NAMES.put(3, "Arena Ranking");
        RANKING_NAMES.put(4, "Wealth Ranking");
        RANKING_NAMES.put(5, "Guild Ranking");
        RANKING_NAMES.put(6, "Pet Ranking");
        RANKING_NAMES.put(7, "Mount Ranking");
        RANKING_NAMES.put(8, "PVP Kills Ranking");
    }

    @Transactional
    public LeaderboardDTO.Response<LeaderboardDTO.RankingInfo> updateScore(LeaderboardDTO.UpdateScoreRequest request) {
        log.info("Updating score: type={}, roleId={}, score={}", 
                request.getRankingType(), request.getRoleId(), request.getScore());

        Optional<RankingEntry> existingOpt = rankingRepository.findByRankingTypeAndRoleId(
                request.getRankingType(), request.getRoleId());

        RankingEntry entry;
        if (existingOpt.isPresent()) {
            entry = existingOpt.get();
            entry.setScore(request.getScore());
            entry.setRoleName(request.getRoleName());
            entry.setRoleLevel(request.getRoleLevel());
            entry.setGuildName(request.getGuildName());
        } else {
            entry = RankingEntry.builder()
                    .rankingType(request.getRankingType())
                    .roleId(request.getRoleId())
                    .roleName(request.getRoleName())
                    .roleLevel(request.getRoleLevel())
                    .score(request.getScore())
                    .guildName(request.getGuildName())
                    .build();
        }

        entry = rankingRepository.save(entry);

        // Update ranks
        updateRanks(request.getRankingType());

        // Clear cache
        clearCache(request.getRankingType());

        return LeaderboardDTO.Response.success(buildRankingInfo(entry));
    }

    @Transactional(readOnly = true)
    public LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse> getLeaderboard(
            Integer rankingType, String currentRoleId) {
        log.info("Getting leaderboard: type={}", rankingType);

        // Try cache first
        String cacheKey = "leaderboard:" + rankingType;
        @SuppressWarnings("unchecked")
        List<LeaderboardDTO.RankingInfo> cached = 
                (List<LeaderboardDTO.RankingInfo>) redisTemplate.opsForValue().get(cacheKey);

        List<LeaderboardDTO.RankingInfo> rankings;
        if (cached != null) {
            log.info("Cache hit for leaderboard: {}", rankingType);
            rankings = cached;
        } else {
            List<RankingEntry> entries = rankingRepository.findTopByType(
                    rankingType, PageRequest.of(0, TOP_LIMIT));
            
            rankings = IntStream.range(0, entries.size())
                    .mapToObj(i -> {
                        RankingEntry entry = entries.get(i);
                        entry.updateRank(i + 1);
                        return buildRankingInfo(entry);
                    })
                    .collect(Collectors.toList());

            // Cache for 5 minutes
            redisTemplate.opsForValue().set(cacheKey, rankings, 5, TimeUnit.MINUTES);
        }

        // Get current player's rank
        LeaderboardDTO.RankingInfo myRank = null;
        if (currentRoleId != null) {
            Optional<RankingEntry> myEntry = rankingRepository.findPlayerRank(rankingType, currentRoleId);
            if (myEntry.isPresent()) {
                RankingEntry entry = myEntry.get();
                Integer rank = rankingRepository.getRankByScore(rankingType, entry.getScore()) + 1;
                entry.updateRank(rank);
                myRank = buildRankingInfo(entry);
            }
        }

        LeaderboardDTO.LeaderboardResponse response = LeaderboardDTO.LeaderboardResponse.builder()
                .rankingType(rankingType)
                .rankingName(RANKING_NAMES.getOrDefault(rankingType, "Unknown"))
                .totalPlayers(rankings.size())
                .rankings(rankings)
                .myRank(myRank)
                .build();

        return LeaderboardDTO.Response.success(response);
    }

    @Transactional
    protected void updateRanks(Integer rankingType) {
        List<RankingEntry> entries = rankingRepository.findTopByType(
                rankingType, PageRequest.of(0, TOP_LIMIT));

        for (int i = 0; i < entries.size(); i++) {
            RankingEntry entry = entries.get(i);
            entry.updateRank(i + 1);
            rankingRepository.save(entry);
        }
    }

    private void clearCache(Integer rankingType) {
        String cacheKey = "leaderboard:" + rankingType;
        redisTemplate.delete(cacheKey);
        log.info("Cache cleared for ranking type: {}", rankingType);
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void refreshAllLeaderboards() {
        log.info("Refreshing all leaderboards...");
        for (Integer type = 1; type <= 8; type++) {
            updateRanks(type);
            clearCache(type);
        }
        log.info("All leaderboards refreshed!");
    }

    private LeaderboardDTO.RankingInfo buildRankingInfo(RankingEntry entry) {
        return LeaderboardDTO.RankingInfo.builder()
                .rank(entry.getCurrentRank())
                .roleId(entry.getRoleId())
                .roleName(entry.getRoleName())
                .roleLevel(entry.getRoleLevel())
                .score(entry.getScore())
                .rankChange(entry.getRankChange())
                .guildName(entry.getGuildName())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
