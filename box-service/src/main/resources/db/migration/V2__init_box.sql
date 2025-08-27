-- V2__init_box.sql

CREATE TABLE IF NOT EXISTS box_state (
                                         role_id            VARCHAR(64) PRIMARY KEY,
    box_level          INT NOT NULL DEFAULT 1,
    box_buy_times      INT NOT NULL DEFAULT 0,
    level_up_end_epoch BIGINT NOT NULL DEFAULT 0,
    level_fetch_flag   INT NOT NULL DEFAULT 0,
    open_box_total     INT NOT NULL DEFAULT 0,
    last_open_is_five  TINYINT NOT NULL DEFAULT 0,
    pending_json       TEXT NULL,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS luck_state (
                                          role_id            VARCHAR(64) PRIMARY KEY,
    start_epoch        BIGINT NOT NULL DEFAULT 0,
    end_epoch          BIGINT NOT NULL DEFAULT 0,
    receive_bitmap     BIGINT NOT NULL DEFAULT 0,
    snapshot_open_cnt  INT NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Thêm cột theo điều kiện (dùng information_schema + dynamic SQL)
SET @db := DATABASE();

-- shi_zhuang_num
SET @exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA=@db AND TABLE_NAME='box_state' AND COLUMN_NAME='shi_zhuang_num');
SET @sql := IF(@exists=0,
  'ALTER TABLE box_state ADD COLUMN shi_zhuang_num INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- arena_item_num
SET @exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA=@db AND TABLE_NAME='box_state' AND COLUMN_NAME='arena_item_num');
SET @sql := IF(@exists=0,
  'ALTER TABLE box_state ADD COLUMN arena_item_num INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_ymd
SET @exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA=@db AND TABLE_NAME='box_state' AND COLUMN_NAME='daily_ymd');
SET @sql := IF(@exists=0,
  'ALTER TABLE box_state ADD COLUMN daily_ymd VARCHAR(16)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- last_open_epoch
SET @exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA=@db AND TABLE_NAME='box_state' AND COLUMN_NAME='last_open_epoch');
SET @sql := IF(@exists=0,
  'ALTER TABLE box_state ADD COLUMN last_open_epoch BIGINT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- box_setting
CREATE TABLE IF NOT EXISTS box_setting (
                                           role_id               VARCHAR(64) PRIMARY KEY,
    equip_eqality         INT NOT NULL DEFAULT 0,
    open_five_mark        INT NOT NULL DEFAULT 0,
    equip_cap_mark        INT NOT NULL DEFAULT 1,
    equip_sell_mark       INT NOT NULL DEFAULT 0,
    condition_first1      INT NOT NULL DEFAULT 0,
    condition_first2      INT NOT NULL DEFAULT 0,
    condition_second1     INT NOT NULL DEFAULT 0,
    condition_second2     INT NOT NULL DEFAULT 0,
    condition_first_mark  INT NOT NULL DEFAULT 0,
    condition_second_mark INT NOT NULL DEFAULT 0,
    retain_mark           INT NOT NULL DEFAULT 0,
    challenge_mark        INT NOT NULL DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
