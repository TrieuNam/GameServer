-- ===========================================
-- V2: Migrate existing bag_meta to new schema
--  - Thêm cột còn thiếu (id, used, created_at, updated_at)
--  - Backfill dữ liệu
--  - Đổi PRIMARY KEY sang (id)
--  - Thêm UNIQUE (role_id, bag_type) nếu thiếu
-- ===========================================

-- 1) Thêm cột nếu thiếu (dùng information_schema, tránh IF NOT EXISTS)
-- id
SET @col := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name   = 'bag_meta'
    AND column_name  = 'id'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE bag_meta ADD COLUMN id VARCHAR(80) NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- used
SET @col := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name   = 'bag_meta'
    AND column_name  = 'used'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE bag_meta ADD COLUMN used INT NOT NULL DEFAULT 0',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- created_at
SET @col := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name   = 'bag_meta'
    AND column_name  = 'created_at'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE bag_meta ADD COLUMN created_at DATETIME(6) NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- updated_at
SET @col := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name   = 'bag_meta'
    AND column_name  = 'updated_at'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE bag_meta ADD COLUMN updated_at DATETIME(6) NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) Backfill giá trị cho các cột mới
UPDATE bag_meta
SET id = CONCAT(role_id, '-', bag_type)
WHERE id IS NULL OR id = '';

UPDATE bag_meta SET created_at = NOW(6) WHERE created_at IS NULL;
UPDATE bag_meta SET updated_at = NOW(6) WHERE updated_at IS NULL;

-- 3) Chuyển PRIMARY KEY sang (id) nếu chưa phải
SET @pk_is_id := (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema   = DATABASE()
    AND table_name     = 'bag_meta'
    AND constraint_name = 'PRIMARY'
    AND column_name    = 'id'
);

-- Nếu PK hiện không phải là 'id', drop PK cũ
SET @sql := IF(@pk_is_id = 0,
  'ALTER TABLE bag_meta DROP PRIMARY KEY',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Set NOT NULL cho id rồi add PK(id) nếu cần
ALTER TABLE bag_meta MODIFY id VARCHAR(80) NOT NULL;

SET @pk_is_id := (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema   = DATABASE()
    AND table_name     = 'bag_meta'
    AND constraint_name = 'PRIMARY'
    AND column_name    = 'id'
);
SET @sql := IF(@pk_is_id = 0,
  'ALTER TABLE bag_meta ADD PRIMARY KEY (id)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) Tạo UNIQUE (role_id, bag_type) nếu chưa có
SET @has_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name   = 'bag_meta'
    AND index_name   = 'uk_role_bag'
);
SET @sql := IF(@has_uk = 0,
  'CREATE UNIQUE INDEX uk_role_bag ON bag_meta (role_id, bag_type)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5) Ràng buộc NOT NULL cho created_at/updated_at sau backfill
ALTER TABLE bag_meta
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;