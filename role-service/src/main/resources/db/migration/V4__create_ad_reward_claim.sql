-- Advertisement reward daily claim log
CREATE TABLE IF NOT EXISTS `ad_reward_claim` (
                                                 `id`            VARCHAR(26)  NOT NULL,
    `user_id`       VARCHAR(64)  NOT NULL,
    `seq`           INT          NOT NULL,
    `is_diamond`    TINYINT(1)   NOT NULL DEFAULT 0,
    `param`         INT          NULL,
    `claim_day`     DATE         NOT NULL, -- UTC day
    `claimed_at`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ad_claim_user_seq_day` (`user_id`, `seq`, `claim_day`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;