-- chat-service consolidated schema (final: BIGINT sender_id/receiver_id/role_id)

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `channel`       INT           NOT NULL,
    `sender_id`     BIGINT        NOT NULL,
    `sender_name`   VARCHAR(50)   NOT NULL,
    `receiver_id`   BIGINT        NULL,
    `receiver_name` VARCHAR(50)   NULL,
    `channel_id`    VARCHAR(50)   NULL,
    `content`       VARCHAR(500)  NOT NULL,
    `created_at`    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_msg_channel`  (`channel`),
    INDEX `idx_msg_sender`   (`sender_id`),
    INDEX `idx_msg_receiver` (`receiver_id`),
    INDEX `idx_msg_time`     (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `muted_player` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT       NOT NULL,
    `role_name`  VARCHAR(50)  NOT NULL,
    `reason`     VARCHAR(200) NULL,
    `mute_until` DATETIME(3)  NOT NULL,
    `muted_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_mute_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
