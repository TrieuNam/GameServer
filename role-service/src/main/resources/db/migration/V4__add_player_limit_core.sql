CREATE TABLE `player_limit_core` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT       NOT NULL,
    `limit_type` INT          NOT NULL COMMENT '1=Mount,2=Angel,3=Gem,4=StarMap,5=Inscription,6=ShenQi',
    `level`      INT          NOT NULL DEFAULT 0,
    `updated_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_limit_type` (`role_id`, `limit_type`),
    KEY `idx_plc_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Limit-core (限界突破) levels per player';
