-- V1__init_lingzhu_tables.sql
-- LingZhu (灵珠副本) dungeon progress
CREATE TABLE IF NOT EXISTS lingzhu_progress (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    role_id     BIGINT  NOT NULL,
    stage       INT     NOT NULL,
    pass_level  INT     NOT NULL DEFAULT 0,
    sweep_count INT     NOT NULL DEFAULT 0,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_stage UNIQUE (role_id, stage),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

