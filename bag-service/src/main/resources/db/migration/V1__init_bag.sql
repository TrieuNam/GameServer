-- bag-service consolidated schema (final: role_id BIGINT, num BIGINT, TIMESTAMP(3), all tables)

CREATE TABLE IF NOT EXISTS bag_items (
    id         VARCHAR(36)  NOT NULL,
    role_id    BIGINT       NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    item_id    INT          NOT NULL,
    num        BIGINT       NOT NULL,
    bind       TINYINT(1)   NOT NULL DEFAULT 0,
    expire_at  TIMESTAMP NULL DEFAULT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_bag_items PRIMARY KEY (id),
    CONSTRAINT uk_role_item_bind_exp UNIQUE (role_id, item_id, bind, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bag_event_dedup (
    event_id   VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recycle_progress (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    role_id    BIGINT       NOT NULL,
    level      INT          NOT NULL DEFAULT 0,
    exp        BIGINT       NOT NULL DEFAULT 0,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_recycle_role UNIQUE (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
