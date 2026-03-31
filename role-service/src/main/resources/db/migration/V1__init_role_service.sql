-- V1__init_role_service.sql
-- Khởi tạo toàn bộ schema cho role-service

-- ============================================================
-- Table: role (entity com.SouthMillion.role_service.entity.Role - BIGINT AUTO_INCREMENT primary key)
-- userId (user-service) = String UUID; roleId (role-service) = Long auto-increment
-- ============================================================

CREATE TABLE IF NOT EXISTS `role` (
    `role_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Auto-increment numeric role ID (dùng cho proto int32/int64)',
    `user_id`       VARCHAR(64)  NOT NULL COMMENT 'UUID của tài khoản từ user-service',
    `name`          VARCHAR(64)  NOT NULL COMMENT 'Tên nhân vật',
    `level`         INT          NOT NULL DEFAULT 1,
    `exp`           BIGINT       NOT NULL DEFAULT 0,
    `hp`            BIGINT       NOT NULL DEFAULT 0  COMMENT 'Lấy từ base_att.hp trong roleexp.json',
    `attack_value`  BIGINT       NOT NULL DEFAULT 0  COMMENT 'Lấy từ base_att.gongji',
    `defense_value` BIGINT       NOT NULL DEFAULT 0  COMMENT 'Lấy từ base_att.fangyu',
    `speed`         INT          NOT NULL DEFAULT 0  COMMENT 'Lấy từ base_att.speed',
    `cap`           BIGINT       NULL,
    `head_pic_id`   INT          NULL,
    `title_id`      INT          NULL,
    `knight_level`  INT          NULL,
    `head_char`     VARCHAR(255) NULL,
    `guild_name`    VARCHAR(64)  NULL,
    `created_at`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_user_name` (`user_id`, `name`),
    INDEX `idx_role_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhân vật người chơi';

-- ============================================================
-- Table: role_system_setting (entity com.SouthMillion.role_service.entity.RoleSystemSetting)
-- ============================================================
CREATE TABLE IF NOT EXISTS `role_system_setting` (
    `user_id`    VARCHAR(64)  NOT NULL,
    `data`       JSON         NOT NULL,
    `updated_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: ads_claim
-- ============================================================
CREATE TABLE IF NOT EXISTS ads_claim (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL,
    seq        INT         NOT NULL,
    day_str    VARCHAR(8)  NOT NULL,
    claimed_at TIMESTAMP   NULL,
    UNIQUE KEY uk_ads_user_seq_day (user_id, seq, day_str),
    INDEX idx_ads_claim_user (user_id),
    INDEX idx_ads_claim_day  (day_str)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: mail
-- ============================================================
CREATE TABLE IF NOT EXISTS `mail` (
    `mail_id`    VARCHAR(26)  NOT NULL,
    `user_id`    VARCHAR(64)  NOT NULL,
    `title`      VARCHAR(128) NOT NULL,
    `content`    TEXT         NULL,
    `items`      JSON         NULL,
    `is_read`    TINYINT(1)   NOT NULL DEFAULT 0,
    `is_fetched` TINYINT(1)   NOT NULL DEFAULT 0,
    `expire_at`  DATETIME(3)  NULL,
    `created_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`mail_id`),
    KEY `idx_mail_user`   (`user_id`),
    KEY `idx_mail_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: ad_reward_claim
-- ============================================================
CREATE TABLE IF NOT EXISTS `ad_reward_claim` (
    `id`         VARCHAR(26) NOT NULL,
    `user_id`    VARCHAR(64) NOT NULL,
    `seq`        INT         NOT NULL,
    `is_diamond` TINYINT(1)  NOT NULL DEFAULT 0,
    `param`      INT         NULL,
    `claim_day`  DATE        NOT NULL COMMENT 'UTC day',
    `claimed_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ad_claim_user_seq_day` (`user_id`, `seq`, `claim_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
