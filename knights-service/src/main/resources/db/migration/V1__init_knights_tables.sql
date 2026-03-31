-- V1__init_knights_tables.sql
-- Knights Handbook (骑士图鉴) system
CREATE TABLE IF NOT EXISTS knights_handbook (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    role_id     BIGINT    NOT NULL,
    level       INT       NOT NULL DEFAULT 1,
    flag        BIGINT    NOT NULL DEFAULT 0,
    level_flag  BIGINT    NOT NULL DEFAULT 0,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_id UNIQUE (role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

