CREATE TABLE IF NOT EXISTS `mail` (
                                      `mail_id`     VARCHAR(26)   NOT NULL,
    `user_id`     VARCHAR(64)   NOT NULL,
    `title`       VARCHAR(128)  NOT NULL,
    `content`     TEXT          NULL,
    `items`       JSON          NULL,
    `is_read`     TINYINT(1)    NOT NULL DEFAULT 0,
    `is_fetched`  TINYINT(1)    NOT NULL DEFAULT 0,
    `expire_at`   DATETIME(3)   NULL,
    `created_at`  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`mail_id`),
    KEY `idx_mail_user` (`user_id`),
    KEY `idx_mail_expire` (`expire_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;