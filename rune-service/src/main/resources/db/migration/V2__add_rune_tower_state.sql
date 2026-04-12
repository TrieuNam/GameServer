CREATE TABLE IF NOT EXISTS `rune_tower_state` (
    `role_id`           BIGINT      NOT NULL,
    `tower_level`       INT         NOT NULL DEFAULT 0,
    `turntable_num`     INT         NOT NULL DEFAULT 0,
    `turntable_flag`    INT         NOT NULL DEFAULT 0,
    `turntable_round`   INT         NOT NULL DEFAULT 0,
    `daily_reward`      TINYINT(1)  NOT NULL DEFAULT 0,
    `pass_reward_index` INT         NOT NULL DEFAULT 0,
    `updated_at`        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
