-- V1__init_gem_tables.sql
-- Gem (宝石) system — fresh install uses BIGINT roleId directly
CREATE TABLE IF NOT EXISTS gem_data (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    role_id    BIGINT      NOT NULL,
    gem_id     INT         NOT NULL,
    level      INT         NOT NULL DEFAULT 1,
    count      INT         NOT NULL DEFAULT 0,
    is_inlaid  TINYINT(1)  NOT NULL DEFAULT 0,
    slot_type  INT         NOT NULL DEFAULT -1,
    updated_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_gem UNIQUE (role_id, gem_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

