CREATE TABLE IF NOT EXISTS `ranking_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ranking_type` INT NOT NULL,
    `role_id` VARCHAR(50) NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `role_level` INT NOT NULL,
    `score` BIGINT NOT NULL DEFAULT 0,
    `current_rank` INT NULL,
    `previous_rank` INT NULL,
    `guild_name` VARCHAR(50) NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_role` (`ranking_type`, `role_id`),
    INDEX `idx_rank_type` (`ranking_type`),
    INDEX `idx_rank_score` (`ranking_type`, `score`),
    INDEX `idx_rank_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
