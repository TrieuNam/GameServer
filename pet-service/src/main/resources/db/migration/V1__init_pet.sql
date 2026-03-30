-- =====================================================
-- Pet Service - Migration V1
-- Date: 2026-02-01
-- Description: Pet system with pets, clothing, gems, remains
-- =====================================================

-- Table: pet_list
-- Main pet collection table
CREATE TABLE IF NOT EXISTS pet_list (
    user_id BIGINT NOT NULL COMMENT 'User owner ID',
    pet_index INT NOT NULL COMMENT 'Pet slot index',
    pet_id INT NOT NULL COMMENT 'Pet template ID',
    level INT NOT NULL DEFAULT 1 COMMENT 'Pet level',
    exp BIGINT NOT NULL DEFAULT 0 COMMENT 'Experience points',
    `order` INT NOT NULL DEFAULT 1 COMMENT 'Display order',
    skill_list VARCHAR(255) COMMENT 'JSON array of skill IDs',
    gem_item_id VARCHAR(255) COMMENT 'JSON array of gem item IDs',
    ts_gem_index VARCHAR(255) COMMENT 'JSON array of TS gem indexes',
    skill_lock_flag INT NOT NULL DEFAULT 0 COMMENT 'Skill lock bitmask',
    cloth_id INT NOT NULL DEFAULT 0 COMMENT 'Equipped clothing ID (0=none)',
    capability BIGINT NOT NULL DEFAULT 0 COMMENT 'Combat power',
    ts_gem_pos_1 INT NULL COMMENT 'TS gem slot 1 position',
    ts_gem_pos_2 INT NULL COMMENT 'TS gem slot 2 position',
    gem_pos_1 INT NULL COMMENT 'Gem slot 1 position',
    gem_pos_2 INT NULL COMMENT 'Gem slot 2 position',
    gem_pos_3 INT NULL COMMENT 'Gem slot 3 position',
    gem_pos_4 INT NULL COMMENT 'Gem slot 4 position',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, pet_index),
    INDEX idx_user_id (user_id),
    INDEX idx_pet_id (pet_id),
    INDEX idx_capability (capability DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Player pet collection';

-- Table: pet_cloth
-- Pet clothing/skins
CREATE TABLE IF NOT EXISTS pet_cloth (
    user_id BIGINT NOT NULL COMMENT 'User owner ID',
    cloth_id INT NOT NULL COMMENT 'Clothing template ID',
    level INT NOT NULL DEFAULT 0 COMMENT 'Clothing level',
    pet_index INT NOT NULL DEFAULT 0 COMMENT '0=not equipped, >0=equipped on pet index',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, cloth_id),
    INDEX idx_user_id (user_id),
    INDEX idx_equipped (user_id, pet_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Pet clothing/skins collection';

-- Table: pet_fight_index
-- Active battle pets selection
CREATE TABLE IF NOT EXISTS pet_fight_index (
    user_id BIGINT NOT NULL PRIMARY KEY COMMENT 'User ID',
    fight_pet_index INT NOT NULL DEFAULT 0 COMMENT 'Primary battle pet index',
    fight_pet_index2 INT NOT NULL DEFAULT 0 COMMENT 'Secondary battle pet index',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Active battle pets configuration';

-- Table: pet_remains
-- Pet remains/relics
CREATE TABLE IF NOT EXISTS pet_remains (
    user_id BIGINT NOT NULL COMMENT 'User owner ID',
    remains_index INT NOT NULL COMMENT 'Remains slot index',
    remains_id INT NOT NULL COMMENT 'Remains template ID',
    grade INT NOT NULL DEFAULT 1 COMMENT 'Quality grade',
    level INT NOT NULL DEFAULT 1 COMMENT 'Remains level',
    exp BIGINT NOT NULL DEFAULT 0 COMMENT 'Experience points',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, remains_index),
    INDEX idx_user_id (user_id),
    INDEX idx_remains_id (remains_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Pet remains/relics collection';

-- Table: pet_ts_gem
-- Special Tian Shi gems with random attributes
CREATE TABLE IF NOT EXISTS pet_ts_gem (
    user_id BIGINT NOT NULL COMMENT 'User owner ID',
    gem_index INT NOT NULL COMMENT 'Gem slot index',
    gem_level INT NOT NULL COMMENT 'Gem level',
    pet_index INT NOT NULL DEFAULT 0 COMMENT '0=not equipped, >0=equipped on pet index',
    attr_type VARCHAR(255) COMMENT 'JSON array of attribute types',
    attr_value VARCHAR(255) COMMENT 'JSON array of attribute values',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, gem_index),
    INDEX idx_user_id (user_id),
    INDEX idx_equipped (user_id, pet_index),
    INDEX idx_gem_level (gem_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Pet special gems (Tian Shi) with random attributes';

-- =====================================================
-- End of Migration V1
-- =====================================================
