-- V1__init_scroll_tables.sql
-- Scroll (法术书/卷轴) system

CREATE TABLE IF NOT EXISTS scroll_meta (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    role_id     BIGINT NOT NULL,
    free_num    INT    NOT NULL DEFAULT 0,
    bao_di_num  INT    NOT NULL DEFAULT 0,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_id UNIQUE (role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scroll_item (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    role_id      BIGINT NOT NULL,
    scroll_index INT    NOT NULL,
    item_id      INT    NOT NULL,
    level        INT    NOT NULL DEFAULT 1,
    wear_mark    INT    NOT NULL DEFAULT 0,
    param        INT    NOT NULL DEFAULT 0,
    updated_at   DATETIME,
    PRIMARY KEY (id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

