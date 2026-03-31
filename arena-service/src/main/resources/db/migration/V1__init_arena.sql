-- Arena Service Database Schema (consolidated V1)
-- Includes: standard arena + cross arena tables

-- Create arena_players table
CREATE TABLE arena_players (
    player_id VARCHAR(50) PRIMARY KEY,
    rating INT NOT NULL DEFAULT 1000,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    current_rank INT NOT NULL DEFAULT 0,
    consecutive_wins INT NOT NULL DEFAULT 0,
    total_battles INT NOT NULL DEFAULT 0,
    season INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_battle_time TIMESTAMP(6) NULL,
    INDEX idx_rating (rating DESC),
    INDEX idx_current_rank (current_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create arena_battle_history table
CREATE TABLE arena_battle_history (
    battle_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player1_id VARCHAR(50) NOT NULL,
    player2_id VARCHAR(50) NOT NULL,
    winner_id VARCHAR(50) NOT NULL,
    rating_change INT NOT NULL,
    player1_rating_before INT NOT NULL,
    player2_rating_before INT NOT NULL,
    player1_rating_after INT NOT NULL,
    player2_rating_after INT NOT NULL,
    battle_duration INT NOT NULL COMMENT 'Battle duration in seconds',
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_player1 (player1_id),
    INDEX idx_player2 (player2_id),
    INDEX idx_timestamp (timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cross Arena tables (CS:9613 PB_CSCrossArenaReq / SC:9614 PB_SCCrossArenaInfo)
CREATE TABLE IF NOT EXISTS cross_arena_player (
    role_id              BIGINT       PRIMARY KEY,
    cross_score          INT          NOT NULL DEFAULT 0,
    today_refresh_times  INT          NOT NULL DEFAULT 0,
    last_refresh_date    DATE         NULL,
    last_refresh_time    INT          NOT NULL DEFAULT 0,
    today_fight_count    INT          NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cross_score (cross_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cross_arena_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    attacker_id     BIGINT       NOT NULL,
    defender_id     BIGINT       NOT NULL,
    is_win          TINYINT(1)   NOT NULL DEFAULT 0,
    attacker_score  INT          NOT NULL DEFAULT 0,
    attacker_change INT          NOT NULL DEFAULT 0,
    defender_score  INT          NOT NULL DEFAULT 0,
    defender_change INT          NOT NULL DEFAULT 0,
    fight_time      BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_attacker       (attacker_id),
    INDEX idx_defender       (defender_id),
    INDEX idx_fight_time     (fight_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

