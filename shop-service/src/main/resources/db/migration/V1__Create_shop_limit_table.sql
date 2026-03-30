-- shop-service consolidated schema (final: BIGINT role_id)

CREATE TABLE IF NOT EXISTS shop_limit (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT      NOT NULL,
    kind        VARCHAR(16) NOT NULL,
    entry_index INT         NOT NULL,
    period      VARCHAR(16) NOT NULL,
    day_str     VARCHAR(16) NOT NULL,
    count       BIGINT      NOT NULL,
    CONSTRAINT uk_shop_limit UNIQUE (role_id, kind, entry_index, period, day_str),
    INDEX idx_shop_limit_role (role_id),
    INDEX idx_shop_limit_day  (day_str)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
