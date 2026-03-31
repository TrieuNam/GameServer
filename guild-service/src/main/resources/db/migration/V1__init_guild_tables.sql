-- guild-service consolidated schema (final: BIGINT for all role/leader IDs; VARCHAR for string-typed IDs)

CREATE TABLE IF NOT EXISTS `guild` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT,
    `name`               VARCHAR(20) NOT NULL,
    `leader_id`          BIGINT      NOT NULL,
    `level`              INT         NOT NULL DEFAULT 1,
    `exp`                BIGINT      NOT NULL DEFAULT 0,
    `member_count`       INT         NOT NULL DEFAULT 1,
    `max_members`        INT         NOT NULL DEFAULT 50,
    `notice`             VARCHAR(500) NULL,
    `tech_attack`        INT         NOT NULL DEFAULT 1,
    `tech_defense`       INT         NOT NULL DEFAULT 1,
    `tech_hp`            INT         NOT NULL DEFAULT 1,
    `tech_crit`          INT         NOT NULL DEFAULT 1,
    `tech_speed`         INT         NOT NULL DEFAULT 1,
    `funds`              BIGINT      NOT NULL DEFAULT 0,
    `donation_reset_time` DATETIME(3) NULL,
    `created_at`         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `disbanded_at`       DATETIME(3) NULL,
    `active`             TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guild_name` (`name`),
    INDEX `idx_guild_name`   (`name`),
    INDEX `idx_guild_level`  (`level`),
    INDEX `idx_guild_leader` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `guild_member` (
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
    `guild_id`             BIGINT      NOT NULL,
    `role_id`              BIGINT      NOT NULL,
    `role_name`            VARCHAR(50) NOT NULL,
    `role_level`           INT         NOT NULL,
    `power`                BIGINT      NOT NULL DEFAULT 0,
    `rank`                 INT         NOT NULL DEFAULT 1,
    `contribution`         BIGINT      NOT NULL DEFAULT 0,
    `daily_donation_count` INT         NOT NULL DEFAULT 0,
    `last_donation_time`   DATETIME(3) NULL,
    `last_online_time`     DATETIME(3) NULL,
    `online`               TINYINT(1)  NOT NULL DEFAULT 0,
    `joined_at`            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guild_role` (`guild_id`, `role_id`),
    INDEX `idx_member_guild` (`guild_id`),
    INDEX `idx_member_role`  (`role_id`),
    INDEX `idx_member_rank`  (`rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `guild_application` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `guild_id`     BIGINT       NOT NULL,
    `role_id`      BIGINT       NOT NULL,
    `role_name`    VARCHAR(50)  NOT NULL,
    `role_level`   INT          NOT NULL,
    `power`        BIGINT       NOT NULL DEFAULT 0,
    `message`      VARCHAR(200) NULL,
    `status`       INT          NOT NULL DEFAULT 0,
    `processor_id` VARCHAR(50)  NULL,
    `processed_at` DATETIME(3)  NULL,
    `applied_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_guild_role` (`guild_id`, `role_id`),
    INDEX `idx_app_guild`  (`guild_id`),
    INDEX `idx_app_role`   (`role_id`),
    INDEX `idx_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `guild_warehouse` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `guild_id`       BIGINT       NOT NULL,
    `item_id`        INT          NOT NULL,
    `item_name`      VARCHAR(100) NOT NULL,
    `quantity`       INT          NOT NULL DEFAULT 1,
    `quality`        INT          NOT NULL DEFAULT 1,
    `depositor_id`   VARCHAR(50)  NOT NULL,
    `depositor_name` VARCHAR(50)  NOT NULL,
    `deposited_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_warehouse_guild` (`guild_id`),
    INDEX `idx_warehouse_item`  (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
