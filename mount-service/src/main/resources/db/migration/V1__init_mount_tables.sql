-- Mount Service Database Schema V1
-- Creates mount + mount_harness tables
-- PB_HarnessData.attr_type/attr_value are repeated[8] per proto msgmount.proto

-- ─── mount ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mount (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    mount_index         INT          NOT NULL DEFAULT 0,
    mount_id            INT          NOT NULL DEFAULT 0,
    level               INT          NOT NULL DEFAULT 0,
    grade               INT          NOT NULL DEFAULT 0,
    exp                 BIGINT       NOT NULL DEFAULT 0,
    is_active           TINYINT(1)   NOT NULL DEFAULT 0,
    is_equipped         TINYINT(1)   NOT NULL DEFAULT 0,
    appearance_id       INT          NULL,
    skin_id             INT          NULL,
    skin_level          INT          NOT NULL DEFAULT 0,
    explore_progress    BIGINT       NOT NULL DEFAULT 0,
    harness_slot_1      INT          NULL,
    harness_slot_2      INT          NULL,
    harness_slot_3      INT          NULL,
    harness_slot_4      INT          NULL,
    star_level          INT          NOT NULL DEFAULT 0,
    lock_flag           INT          NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id        (user_id),
    INDEX idx_user_mount     (user_id, mount_index),
    UNIQUE KEY uk_user_mount (user_id, mount_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── mount_harness ────────────────────────────────────────────────────────────
-- PB_HarnessData: attr_type[8] + attr_value[8] + lock_flag (bitmask 0-7)
-- Java entity: entry1_type..entry4_type (4 slots) — need to add entry5..entry8
CREATE TABLE IF NOT EXISTS mount_harness (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT     NOT NULL,
    harness_index   INT        NOT NULL,   -- bag slot index
    item_id         INT        NOT NULL DEFAULT 0,
    level           INT        NOT NULL DEFAULT 0,
    grade           INT        NOT NULL DEFAULT 0,
    wearing_mark    TINYINT(1) NOT NULL DEFAULT 0,   -- 1=equipped
    attr_num        INT        NOT NULL DEFAULT 0,   -- number of active attr slots

    -- 8 attribute slots (PB_HarnessData.attr_type[8] / attr_value[8])
    entry1_type     INT,
    entry1_value    BIGINT,
    entry2_type     INT,
    entry2_value    BIGINT,
    entry3_type     INT,
    entry3_value    BIGINT,
    entry4_type     INT,
    entry4_value    BIGINT,
    entry5_type     INT,        -- ADDED (slots 5-8 were missing in old entity)
    entry5_value    BIGINT,
    entry6_type     INT,
    entry6_value    BIGINT,
    entry7_type     INT,
    entry7_value    BIGINT,
    entry8_type     INT,
    entry8_value    BIGINT,

    lock_flag       INT        NOT NULL DEFAULT 0,   -- bitmask, bits 0-7
    created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id        (user_id),
    INDEX idx_user_harness   (user_id, harness_index),
    UNIQUE KEY uk_user_harness (user_id, harness_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

