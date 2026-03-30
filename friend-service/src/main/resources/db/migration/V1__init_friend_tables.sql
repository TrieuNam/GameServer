-- friend-service consolidated schema (final: BIGINT for all role/player IDs)

CREATE TABLE IF NOT EXISTS `friend` (
    `id`                   BIGINT    NOT NULL AUTO_INCREMENT,
    `role_id1`             BIGINT    NOT NULL,
    `role_name1`           VARCHAR(50) NOT NULL,
    `role_id2`             BIGINT    NOT NULL,
    `role_name2`           VARCHAR(50) NOT NULL,
    `friendship_level`     INT       NOT NULL DEFAULT 1,
    `friendship_points`    BIGINT    NOT NULL DEFAULT 0,
    `last_interaction_time` DATETIME(3) NULL,
    `created_at`           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_friend_pair` (`role_id1`, `role_id2`),
    INDEX `idx_friend_role1` (`role_id1`),
    INDEX `idx_friend_role2` (`role_id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `friend_request` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `sender_id`    BIGINT      NOT NULL,
    `sender_name`  VARCHAR(50) NOT NULL,
    `sender_level` INT         NOT NULL,
    `receiver_id`  BIGINT      NOT NULL,
    `receiver_name` VARCHAR(50) NOT NULL,
    `message`      VARCHAR(200) NULL,
    `status`       INT         NOT NULL DEFAULT 0,
    `processed_at` DATETIME(3) NULL,
    `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_pair` (`sender_id`, `receiver_id`),
    INDEX `idx_request_sender`   (`sender_id`),
    INDEX `idx_request_receiver` (`receiver_id`),
    INDEX `idx_request_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `blocked_player` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `blocker_id`   BIGINT      NOT NULL,
    `blocked_id`   BIGINT      NOT NULL,
    `blocked_name` VARCHAR(50) NOT NULL,
    `reason`       VARCHAR(200) NULL,
    `blocked_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_block_pair` (`blocker_id`, `blocked_id`),
    INDEX `idx_blocked_blocker` (`blocker_id`),
    INDEX `idx_blocked_target`  (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `online_status` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT,
    `role_id`          BIGINT      NOT NULL,
    `role_name`        VARCHAR(50) NOT NULL,
    `level`            INT         NOT NULL,
    `online`           TINYINT(1)  NOT NULL DEFAULT 0,
    `last_login_time`  DATETIME(3) NULL,
    `last_logout_time` DATETIME(3) NULL,
    `updated_at`       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_id` (`role_id`),
    INDEX `idx_status_role`   (`role_id`),
    INDEX `idx_status_online` (`online`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
