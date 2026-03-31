-- mail-service consolidated schema (final: BIGINT receiver_id/sender_id)

CREATE TABLE IF NOT EXISTS `mail` (
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
    `type`                 INT         NOT NULL,
    `sender_id`            BIGINT      NULL,
    `sender_name`          VARCHAR(50) NULL,
    `receiver_id`          BIGINT      NOT NULL,
    `title`                VARCHAR(100) NOT NULL,
    `content`              VARCHAR(1000) NULL,
    `is_read`              TINYINT(1)  NOT NULL DEFAULT 0,
    `is_claimed_attachment` TINYINT(1) NOT NULL DEFAULT 0,
    `is_deleted`           TINYINT(1)  NOT NULL DEFAULT 0,
    `expires_at`           DATETIME(3) NOT NULL,
    `created_at`           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `read_at`              DATETIME(3) NULL,
    `claimed_at`           DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_mail_receiver` (`receiver_id`),
    INDEX `idx_mail_status`   (`is_deleted`, `is_read`),
    INDEX `idx_mail_expire`   (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mail_attachment` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `mail_id`         BIGINT      NOT NULL,
    `attachment_type` INT         NOT NULL,
    `item_id`         VARCHAR(50) NULL,
    `item_name`       VARCHAR(100) NULL,
    `quantity`        INT         NOT NULL DEFAULT 1,
    `quality`         INT         NULL,
    `created_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_attachment_mail` (`mail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
