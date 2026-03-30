-- V1__init_pagoda_tables.sql
-- Pagoda system: ShiLian (试炼之塔) + GuMo (锢魔之塔)

CREATE TABLE IF NOT EXISTS shilian_progress (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    role_id         BIGINT  NOT NULL,
    pass_level      INT     NOT NULL DEFAULT 0,
    best_level      INT     NOT NULL DEFAULT 0,
    use_item        INT     NOT NULL DEFAULT 0,
    random_id       INT     NOT NULL DEFAULT 0,
    season_end_time BIGINT  NOT NULL DEFAULT 0,
    updated_at      DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_id UNIQUE (role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS gumo_layer (
    id         BIGINT     NOT NULL AUTO_INCREMENT,
    role_id    BIGINT     NOT NULL,
    layer_id   INT        NOT NULL,
    star_flag  INT        NOT NULL DEFAULT 0,
    box_flag   TINYINT(1) NOT NULL DEFAULT 0,
    updated_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_layer UNIQUE (role_id, layer_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS gumo_meta (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    role_id      BIGINT NOT NULL,
    day_reward   INT    NOT NULL DEFAULT 0,
    lastday_level INT   NOT NULL DEFAULT 0,
    updated_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_id UNIQUE (role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

