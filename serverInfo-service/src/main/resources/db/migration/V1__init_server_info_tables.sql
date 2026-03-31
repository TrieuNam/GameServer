CREATE TABLE IF NOT EXISTS `server_info` (
    `id` INT NOT NULL,
    `real_start_time` BIGINT NOT NULL,
    `real_combine_time` BIGINT NOT NULL DEFAULT 0,
    `updated_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default server info row if not exists
INSERT IGNORE INTO `server_info` (`id`, `real_start_time`, `real_combine_time`)
VALUES (1, UNIX_TIMESTAMP() * 1000, 0);
