-- =====================================================
-- Main FB Service - Migration V1
-- Date: 2026-02-01
-- Description: Main story progression tracking
-- =====================================================

-- Table: main_fb_stage_progress
-- Tracks player progress through story stages
CREATE TABLE IF NOT EXISTS main_fb_stage_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id VARCHAR(64) NOT NULL COMMENT 'Player identifier',
    stage INT NOT NULL COMMENT 'Chapter number',
    level INT NOT NULL COMMENT 'Stage within chapter',
    best_score INT COMMENT 'Best score/stars achieved',
    clear_count INT DEFAULT 0 COMMENT 'Number of completions',
    first_clear_ts BIGINT COMMENT 'First clear timestamp (epoch seconds)',
    last_clear_ts BIGINT COMMENT 'Last clear timestamp (epoch seconds)',
    
    CONSTRAINT uk_progress_player_stage_level UNIQUE (player_id, stage, level),
    INDEX idx_player (player_id),
    INDEX idx_stage_level (stage, level),
    INDEX idx_last_clear (last_clear_ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Player stage progress tracking';

-- Table: mainfb_task_progress
-- Tracks player task completion progress
CREATE TABLE IF NOT EXISTS mainfb_task_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Player identifier',
    done_index INT COMMENT 'Highest completed task index (1-based)',
    updated_at BIGINT NOT NULL COMMENT 'Last update timestamp (epoch seconds)',
    
    INDEX idx_player (player_id),
    INDEX idx_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Player task progress tracking';

-- Table: main_fb_chapter_reward
-- Tracks chapter reward claims
CREATE TABLE IF NOT EXISTS main_fb_chapter_reward (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id VARCHAR(64) NOT NULL COMMENT 'Player identifier',
    stage INT NOT NULL COMMENT 'Chapter index',
    chapter_label VARCHAR(32) NOT NULL COMMENT 'Chapter label (e.g., 1-10)',
    claimed TINYINT(1) DEFAULT 0 COMMENT 'Reward claimed flag',
    claim_ts BIGINT COMMENT 'Claim timestamp (epoch seconds)',
    
    CONSTRAINT uk_chapter_reward UNIQUE (player_id, stage, chapter_label),
    INDEX idx_player (player_id),
    INDEX idx_claimed (claimed),
    INDEX idx_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Chapter reward claim tracking';

-- =====================================================
-- End of Migration V1
-- =====================================================
