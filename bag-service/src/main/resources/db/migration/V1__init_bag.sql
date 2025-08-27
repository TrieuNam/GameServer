-- =========================
-- V1: Fresh install schema
-- =========================

CREATE TABLE IF NOT EXISTS bag_meta (
                                        id          VARCHAR(80)  NOT NULL,
    role_id     VARCHAR(64)  NOT NULL,
    bag_type    TINYINT      NOT NULL,
    capacity    INT          NOT NULL,
    used        INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    version     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_bag (role_id, bag_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bag_slot (
                                        id          BIGINT       NOT NULL AUTO_INCREMENT,
                                        role_id     VARCHAR(64)  NOT NULL,
    bag_type    TINYINT      NOT NULL,
    slot_index  INT          NOT NULL,
    item_id     INT          NOT NULL,
    count       BIGINT       NOT NULL,
    bind        TINYINT      NOT NULL DEFAULT 0,
    expire_at   DATETIME(6)  NULL,
    extra_json  JSON         NULL,
    version     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_bag_slot (role_id, bag_type, slot_index),
    KEY idx_role_bag (role_id, bag_type),
    KEY idx_role_bag_item (role_id, bag_type, item_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;