-- System settings per user (JSON payload)
CREATE TABLE IF NOT EXISTS `role_system_setting` (
                                                     `user_id`    VARCHAR(64)  NOT NULL,
    `data`       JSON         NOT NULL,
    `updated_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;