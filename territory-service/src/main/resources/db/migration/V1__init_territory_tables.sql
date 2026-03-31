-- ═══════════════════════════════════════════════════════
--  Territory Service — V1 Initial Schema
--  Tables: territory, territory_building
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS `territory` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`               BIGINT      NOT NULL COMMENT 'Player user ID (unique per player)',
    `territory_id`          INT         NOT NULL COMMENT 'Territory config ID (type/location)',
    `level`                 INT         NOT NULL DEFAULT 1  COMMENT 'Territory level',
    `name`                  VARCHAR(50) NULL     COMMENT 'Customizable territory name',
    `prosperity`            BIGINT      NOT NULL DEFAULT 0  COMMENT 'Prosperity/development score',
    `defense_rating`        BIGINT      NOT NULL DEFAULT 0  COMMENT 'Total defense rating',
    `attack_rating`         BIGINT      NOT NULL DEFAULT 0  COMMENT 'Total attack rating',
    `gold_production`       BIGINT      NOT NULL DEFAULT 0  COMMENT 'Gold production per hour',
    `resource_production`   BIGINT      NOT NULL DEFAULT 0  COMMENT 'Resource production per hour',
    `accumulated_gold`      BIGINT      NOT NULL DEFAULT 0  COMMENT 'Accumulated (uncollected) gold',
    `accumulated_resources` BIGINT      NOT NULL DEFAULT 0  COMMENT 'Accumulated (uncollected) resources',
    `last_production_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Last time production was calculated',
    `max_gold_storage`      BIGINT      NOT NULL DEFAULT 10000 COMMENT 'Max gold storage capacity',
    `max_resource_storage`  BIGINT      NOT NULL DEFAULT 10000 COMMENT 'Max resource storage capacity',
    `building_slots`        INT         NOT NULL DEFAULT 5  COMMENT 'Number of building slots',
    `appearance_id`         INT         NOT NULL DEFAULT 0  COMMENT 'Appearance/skin ID',
    `created_at`            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `territory_building` (
    `id`                       BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`                  BIGINT      NOT NULL COMMENT 'Owner user ID',
    `slot_id`                  INT         NOT NULL COMMENT 'Building slot/position in territory',
    `building_id`              INT         NOT NULL DEFAULT 0 COMMENT 'Building config/type ID',
    `level`                    INT         NOT NULL DEFAULT 0 COMMENT 'Building level',
    `status`                   INT         NOT NULL DEFAULT 0 COMMENT '0=Empty 1=Constructing 2=Built 3=Upgrading',
    `start_time`               DATETIME(3) NULL     COMMENT 'Construction/upgrade start time',
    `finish_time`              DATETIME(3) NULL     COMMENT 'Construction/upgrade finish time',
    `production_rate`          BIGINT      NOT NULL DEFAULT 0 COMMENT 'Production rate if production building',
    `defense_contribution`     BIGINT      NOT NULL DEFAULT 0 COMMENT 'Defense stat contribution',
    `attack_contribution`      BIGINT      NOT NULL DEFAULT 0 COMMENT 'Attack stat contribution',
    `prosperity_contribution`  BIGINT      NOT NULL DEFAULT 0 COMMENT 'Prosperity contribution',
    `created_at`               DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`               DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_slot` (`user_id`, `slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
