package com.SouthMillion.arenaservice.service;

import com.SouthMillion.arenaservice.client.RoleFeignClient;
import com.SouthMillion.arenaservice.dto.ArenaPlayerDTO;
import com.SouthMillion.arenaservice.dto.BattleRequest;
import com.SouthMillion.arenaservice.dto.BattleResult;
import com.SouthMillion.arenaservice.entity.ArenaBattleHistory;
import com.SouthMillion.arenaservice.entity.ArenaPlayer;
import com.SouthMillion.arenaservice.repository.ArenaBattleHistoryRepository;
import com.SouthMillion.arenaservice.repository.ArenaPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaService {

    private final ArenaPlayerRepository arenaPlayerRepository;
    private final ArenaBattleHistoryRepository battleHistoryRepository;
    private final RoleFeignClient roleFeignClient;
    private final Random random = new Random();

    private static final Integer CURRENT_SEASON = 1;
    private static final Integer RATING_RANGE = 200; // Matchmaking range
    private static final Integer BASE_RATING_CHANGE = 20;
    private static final Integer CONSECUTIVE_WIN_BONUS = 5;

    @Transactional
    public ArenaPlayer getOrCreatePlayer(String playerId) {
        return arenaPlayerRepository.findByPlayerId(playerId)
                .orElseGet(() -> {
                    ArenaPlayer newPlayer = ArenaPlayer.builder()
                            .playerId(playerId)
                            .rating(1000)
                            .wins(0)
                            .losses(0)
                            .currentRank(0)
                            .consecutiveWins(0)
                            .totalBattles(0)
                            .season(CURRENT_SEASON)
                            .build();
                    return arenaPlayerRepository.save(newPlayer);
                });
    }

    @Cacheable(value = "arenaPlayer", key = "#playerId")
    public Optional<ArenaPlayer> getPlayer(String playerId) {
        return arenaPlayerRepository.findByPlayerId(playerId);
    }

    @Cacheable(value = "arenaRankings", key = "#season")
    public List<ArenaPlayerDTO> getTop100Rankings(Integer season) {
        return arenaPlayerRepository.findTop100BySeasonOrderByRatingDesc(season)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Cacheable(value = "arenaRankings", key = "'page:' + #page + ':' + #season")
    public Page<ArenaPlayerDTO> getRankings(Integer season, int page, int size) {
        return arenaPlayerRepository.findBySeasonOrderByRatingDesc(season, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    public Integer calculatePlayerRank(String playerId) {
        ArenaPlayer player = getOrCreatePlayer(playerId);
        Integer rank = arenaPlayerRepository.calculateRank(player.getRating(), player.getSeason());
        
        // Update rank
        player.setCurrentRank(rank);
        arenaPlayerRepository.save(player);
        
        return rank;
    }

    public Optional<ArenaPlayer> findOpponent(String playerId) {
        ArenaPlayer player = getOrCreatePlayer(playerId);
        Integer minRating = Math.max(0, player.getRating() - RATING_RANGE);
        Integer maxRating = player.getRating() + RATING_RANGE;
        
        return arenaPlayerRepository.findRandomOpponent(CURRENT_SEASON, minRating, maxRating)
                .filter(opponent -> !opponent.getPlayerId().equals(playerId));
    }

    /**
     * Return up to {@code count} distinct random opponents near player's rating.
     * Falls back to empty list when the player pool is too small.
     */
    public List<ArenaPlayer> findOpponents(String playerId, int count) {
        ArenaPlayer player = getOrCreatePlayer(playerId);
        Integer minRating = Math.max(0, player.getRating() - RATING_RANGE);
        Integer maxRating = player.getRating() + RATING_RANGE;
        return arenaPlayerRepository.findRandomOpponents(CURRENT_SEASON, minRating, maxRating, playerId, count);
    }

    @Transactional
    @CacheEvict(value = {"arenaPlayer", "arenaRankings"}, allEntries = true)
    public BattleResult processBattle(BattleRequest request) {
        ArenaPlayer player = getOrCreatePlayer(request.getPlayerId());
        
        // Find opponent
        ArenaPlayer opponent;
        if (request.getOpponentId() != null) {
            opponent = getOrCreatePlayer(request.getOpponentId());
        } else {
            opponent = findOpponent(request.getPlayerId())
                    .orElseThrow(() -> new IllegalStateException("No opponent found"));
        }

        // Simulate battle (simplified: higher rating has better chance)
        // Inject fight power from role-service if not already provided in request
        long attackerPower = request.getAttackerPower();
        long defenderPower = request.getDefenderPower();
        if (attackerPower <= 0) {
            attackerPower = fetchFightPower(player.getPlayerId());
        }
        if (defenderPower <= 0) {
            defenderPower = fetchFightPower(opponent.getPlayerId());
        }
        boolean playerWins = simulateBattle(player, opponent, attackerPower, defenderPower);

        String winnerId = playerWins ? player.getPlayerId() : opponent.getPlayerId();
        String loserId = playerWins ? opponent.getPlayerId() : player.getPlayerId();
        
        ArenaPlayer winner = playerWins ? player : opponent;
        ArenaPlayer loser = playerWins ? opponent : player;

        // Calculate rating change (ELO-like system)
        Integer ratingChange = calculateRatingChange(winner.getRating(), loser.getRating());
        
        // Apply consecutive win bonus
        Integer consecutiveBonus = 0;
        if (playerWins && player.getConsecutiveWins() >= 3) {
            consecutiveBonus = CONSECUTIVE_WIN_BONUS * (player.getConsecutiveWins() / 3);
            ratingChange += consecutiveBonus;
        }

        // Store ratings before change
        Integer winnerRatingBefore = winner.getRating();
        Integer loserRatingBefore = loser.getRating();

        // Update winner
        winner.setRating(winner.getRating() + ratingChange);
        winner.setWins(winner.getWins() + 1);
        winner.setConsecutiveWins(winner.getConsecutiveWins() + 1);
        winner.setTotalBattles(winner.getTotalBattles() + 1);
        winner.setLastBattleTime(Instant.now());

        // Update loser
        loser.setRating(Math.max(0, loser.getRating() - ratingChange));
        loser.setLosses(loser.getLosses() + 1);
        loser.setConsecutiveWins(0); // Reset consecutive wins
        loser.setTotalBattles(loser.getTotalBattles() + 1);
        loser.setLastBattleTime(Instant.now());

        arenaPlayerRepository.save(winner);
        arenaPlayerRepository.save(loser);

        // Save battle history
        Integer battleDuration = 30 + random.nextInt(120); // 30-150 seconds
        ArenaBattleHistory history = ArenaBattleHistory.builder()
                .player1Id(player.getPlayerId())
                .player2Id(opponent.getPlayerId())
                .winnerId(winnerId)
                .ratingChange(ratingChange)
                .player1RatingBefore(player.getPlayerId().equals(winnerId) ? winnerRatingBefore : loserRatingBefore)
                .player2RatingBefore(opponent.getPlayerId().equals(winnerId) ? winnerRatingBefore : loserRatingBefore)
                .player1RatingAfter(player.getRating())
                .player2RatingAfter(opponent.getRating())
                .battleDuration(battleDuration)
                .build();
        battleHistoryRepository.save(history);

        // Update ranks
        calculatePlayerRank(player.getPlayerId());
        calculatePlayerRank(opponent.getPlayerId());

        return BattleResult.builder()
                .winnerId(winnerId)
                .loserId(loserId)
                .ratingChange(ratingChange)
                .winnerRatingBefore(winnerRatingBefore)
                .winnerRatingAfter(winner.getRating())
                .loserRatingBefore(loserRatingBefore)
                .loserRatingAfter(loser.getRating())
                .battleDuration(battleDuration)
                .isConsecutiveWin(playerWins && player.getConsecutiveWins() > 1)
                .consecutiveWinBonus(consecutiveBonus)
                .build();
    }

    /**
     * Battle resolution formula — matches C++ BattleServer logic (H5 version).
     *
     * Priority:
     *  1. If fightPower data available → power difference:
     *       winChance = 0.5 + clamp(powerDiff / totalPower * 0.4, -0.4, +0.4)
     *  2. Rating difference adds secondary modifier:
     *       winChance += clamp(ratingDiff / 800.0 * 0.1, -0.1, +0.1)
     *  3. Streak bonus: +2% per win, max +10%
     *  4. Final clamp: [0.10, 0.90]
     */
    private boolean simulateBattle(ArenaPlayer player1, ArenaPlayer player2,
                                   long p1Power, long p2Power) {
        double winChance = 0.5;

        // Fight power factor (primary when available)
        if (p1Power > 0 && p2Power > 0) {
            long totalPower = p1Power + p2Power;
            double powerDiff = (double)(p1Power - p2Power);
            double powerFactor = Math.max(-0.40, Math.min(0.40, powerDiff / totalPower * 0.4));
            winChance += powerFactor;
        }

        // Rating secondary modifier
        int ratingDiff = player1.getRating() - player2.getRating();
        double ratingFactor = Math.max(-0.35, Math.min(0.35, ratingDiff / 600.0));
        winChance += ratingFactor * 0.1; // weight 10% vs power

        // Streak bonus
        int streak = player1.getConsecutiveWins();
        if (streak >= 2) {
            winChance += Math.min(0.10, streak * 0.02);
        }

        winChance = Math.max(0.10, Math.min(0.90, winChance));

        log.debug("[battle] p1={} p2={} power={}/{} ratingDiff={} winChance={}",
                player1.getPlayerId(), player2.getPlayerId(), p1Power, p2Power,
                ratingDiff, String.format("%.2f", winChance));

        return random.nextDouble() < winChance;
    }

    /** Fetch fight power from role-service; fallback 0 on error */
    private long fetchFightPower(String roleId) {
        try {
            Map<String, Object> data = roleFeignClient.getCombatPower(roleId);
            Object fp = data.get("fightPower");
            if (fp instanceof Number) return ((Number) fp).longValue();
        } catch (Exception e) {
            log.warn("[arena] fetchFightPower failed for roleId={}: {}", roleId, e.getMessage());
        }
        return 0L;
    }

    private Integer calculateRatingChange(Integer winnerRating, Integer loserRating) {
        // ELO-like system with 3-tier bracket:
        //   upset (winner << loser): +30 pts
        //   even   (±200):           +20 pts (BASE)
        //   stomp  (winner >> loser): +10 pts
        int diff = winnerRating - loserRating;
        if (diff > 200) return 10;      // stomp
        if (diff < -200) return 30;     // upset
        // Linear scale within ±200: 10→30
        return BASE_RATING_CHANGE + (int) ((-diff / 200.0) * 10);
    }


    public Page<ArenaBattleHistory> getPlayerBattleHistory(String playerId, int page, int size) {
        return battleHistoryRepository.findPlayerHistory(playerId, PageRequest.of(page, size));
    }

    public ArenaPlayerDTO toDTO(ArenaPlayer player) {
        return ArenaPlayerDTO.builder()
                .playerId(player.getPlayerId())
                .rating(player.getRating())
                .wins(player.getWins())
                .losses(player.getLosses())
                .currentRank(player.getCurrentRank())
                .consecutiveWins(player.getConsecutiveWins())
                .totalBattles(player.getTotalBattles())
                .winRate(player.getWinRate())
                .season(player.getSeason())
                .build();
    }

    private static final int MAX_DAILY_CHALLENGES = 10;

    /**
     * Get remaining challenges for today (resets daily at midnight)
     */
    public int getChallengesRemaining(String playerId) {
        ArenaPlayer player = getOrCreatePlayer(playerId);
        LocalDate today = LocalDate.now();
        if (player.getLastResetDate() == null || !today.equals(player.getLastResetDate())) {
            return MAX_DAILY_CHALLENGES;
        }
        return Math.max(0, MAX_DAILY_CHALLENGES - player.getChallengesUsedToday());
    }

    /**
     * Consume one challenge. Throws if daily limit reached.
     */
    @Transactional
    public void consumeChallenge(String playerId) {
        ArenaPlayer player = arenaPlayerRepository.findByPlayerId(playerId)
                .orElseGet(() -> getOrCreatePlayer(playerId));
        LocalDate today = LocalDate.now();
        if (player.getLastResetDate() == null || !today.equals(player.getLastResetDate())) {
            player.setChallengesUsedToday(0);
            player.setLastResetDate(today);
        }
        if (player.getChallengesUsedToday() >= MAX_DAILY_CHALLENGES) {
            throw new IllegalStateException("Daily challenge limit reached (max " + MAX_DAILY_CHALLENGES + ")");
        }
        player.setChallengesUsedToday(player.getChallengesUsedToday() + 1);
        arenaPlayerRepository.save(player);
    }

    /**
     * Add extra challenges (bought by player). Decrements challengesUsedToday.
     */
    @Transactional
    public int addBoughtChallenges(String playerId, int count) {
        ArenaPlayer player = arenaPlayerRepository.findByPlayerId(playerId)
                .orElseGet(() -> getOrCreatePlayer(playerId));
        LocalDate today = LocalDate.now();
        if (player.getLastResetDate() == null || !today.equals(player.getLastResetDate())) {
            player.setChallengesUsedToday(0);
            player.setLastResetDate(today);
        }
        int newUsed = Math.max(0, player.getChallengesUsedToday() - count);
        player.setChallengesUsedToday(newUsed);
        arenaPlayerRepository.save(player);
        return Math.max(0, MAX_DAILY_CHALLENGES - newUsed);
    }

    /**
     * Calculate gold reward based on player's current rank
     */
    public long calculateRankReward(String playerId) {
        ArenaPlayer player = getOrCreatePlayer(playerId);
        int rank = player.getCurrentRank();
        if (rank <= 0) {
            rank = calculatePlayerRank(playerId);
        }
        if (rank <= 10)  return 10_000L;
        if (rank <= 50)  return 5_000L;
        if (rank <= 100) return 2_000L;
        return 500L;
    }
}
