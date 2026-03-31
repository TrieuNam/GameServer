-- Flyway migration for artifact-service
-- Creates: artifact, artifact_draw_record tables

-- ─── artifact ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS artifact (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    artifact_index    INT          NOT NULL,
    artifact_id       INT          NOT NULL,
    level             INT          NOT NULL DEFAULT 0,
    grade             INT          NOT NULL DEFAULT 0,
    exp               BIGINT       NOT NULL DEFAULT 0,
    is_active         TINYINT(1)   NOT NULL DEFAULT 0,
    is_equipped       TINYINT(1)   NOT NULL DEFAULT 0,
    refinement_level  INT          NOT NULL DEFAULT 0,
    awakening_stage   INT          NOT NULL DEFAULT 0,
    soul_power        BIGINT       NOT NULL DEFAULT 0,
    divine_essence    BIGINT       NOT NULL DEFAULT 0,
    attr1_type        INT,
    attr1_value       BIGINT,
    attr2_type        INT,
    attr2_value       BIGINT,
    attr3_type        INT,
    attr3_value       BIGINT,
    attr4_type        INT,
    attr4_value       BIGINT,
    skill1_level      INT          NOT NULL DEFAULT 0,
    skill2_level      INT          NOT NULL DEFAULT 0,
    skill3_level      INT          NOT NULL DEFAULT 0,
    blessing_tier     INT          NOT NULL DEFAULT 0,
    lock_attr_flag    INT          NOT NULL DEFAULT 0,
    pity_counter      INT          NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id        (user_id),
    INDEX idx_user_artifact  (user_id, artifact_index),
    UNIQUE KEY uk_user_artifact (user_id, artifact_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── artifact_draw_record ──────────────────────────────────────────────────────
-- Used by ShenQi gacha: op=8 DRAW, op=9 GET_RECORDS
-- Matches C++ SC:1680 PB_SCShenQiRecordInfo / PB_ShenQiRecordData
CREATE TABLE IF NOT EXISTS artifact_draw_record (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    draw_type        INT          NOT NULL,    -- 1=single, 10=10-pull
    artifact_id      INT          NOT NULL,    -- artifact obtained
    quality          INT          NOT NULL DEFAULT 1,
    is_guaranteed    TINYINT(1)   NOT NULL DEFAULT 0,
    draw_timestamp   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cost_type        INT          NOT NULL DEFAULT 0,
    cost_amount      BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id         (user_id),
    INDEX idx_user_timestamp  (user_id, draw_timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

