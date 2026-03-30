-- V2__add_role_skill_and_role_talent.sql
-- Bổ sung bảng persistence cho hệ thống skill / talent của role-service

CREATE TABLE IF NOT EXISTS `role_skill` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_id`     BIGINT       NOT NULL,
    `skill_id`    INT          NOT NULL,
    `skill_level` INT          NOT NULL DEFAULT 1,
    `skill_index` INT          NOT NULL DEFAULT 0,
    `updated_at`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_skill` (`role_id`, `skill_id`),
    KEY `idx_role_skill_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `role_talent` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_id`     BIGINT       NOT NULL,
    `skill_id`    INT          NOT NULL,
    `skill_level` INT          NOT NULL DEFAULT 1,
    `updated_at`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_talent` (`role_id`, `skill_id`),
    KEY `idx_role_talent_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

