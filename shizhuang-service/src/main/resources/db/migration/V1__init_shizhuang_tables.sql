-- V1__init_shizhuang_tables.sql
-- Shizhuang (时装) system: player_shizhuang, player_clothes, shizhuang

CREATE TABLE IF NOT EXISTS player_shizhuang (
    id           BIGINT     NOT NULL AUTO_INCREMENT,
    role_id      BIGINT     NOT NULL,
    shizhuang_id INT        NOT NULL,
    level        INT        NOT NULL DEFAULT 1,
    star         INT        NOT NULL DEFAULT 1,
    activated    TINYINT(1) NOT NULL DEFAULT 0,
    wearing      TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_shizhuang UNIQUE (role_id, shizhuang_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS player_clothes (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    player_id  BIGINT NOT NULL,
    clothes_id INT    NOT NULL,
    level      INT    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uq_player_clothes UNIQUE (player_id, clothes_id),
    INDEX idx_player_id (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shizhuang (
    id      INT    NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    level   INT    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

