-- Flyway Migration V1: Initialize block-service database schema
-- Purpose: Create tables for role block state and block nodes

-- Table: role_block_state
-- Stores per-role block system state (activate seq, wearing map, next index)
CREATE TABLE IF NOT EXISTS `role_block_state` (
    `role_id`          BIGINT       NOT NULL PRIMARY KEY,
    `activate_seq`     INT          NOT NULL DEFAULT 0,
    `wearing_map_id`   INT          NOT NULL DEFAULT 0,
    `next_block_index` INT          NOT NULL DEFAULT 1,
    `updated_at`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Role block state: tracks activation seq, wearing map, next index';

-- Table: block_node
-- Stores individual block placements on maps
CREATE TABLE IF NOT EXISTS `block_node` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `role_id`      BIGINT       NOT NULL,
    `block_id`     INT          NOT NULL,
    `map_id`       INT          NOT NULL,
    `block_index`  INT          NOT NULL,
    `pos_x`        INT          NOT NULL DEFAULT 0,
    `pos_y`        INT          NOT NULL DEFAULT 0,
    `color`        INT          NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_block_role_map_index` (`role_id`, `map_id`, `block_index`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_role_map` (`role_id`, `map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Block node placement: stores x/y pos, color, block type per map';
