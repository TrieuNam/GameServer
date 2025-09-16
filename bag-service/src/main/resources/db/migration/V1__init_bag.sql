CREATE TABLE IF NOT EXISTS bag_items (
                                         id         VARCHAR(36)  NOT NULL,
    role_id    VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    item_id    INT          NOT NULL,
    num        INT          NOT NULL,
    bind       TINYINT(1)   NOT NULL DEFAULT 0,
    expire_at  TIMESTAMP NULL DEFAULT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_bag_items PRIMARY KEY (id),
    CONSTRAINT uk_role_item_bind_exp UNIQUE (role_id, item_id, bind, expire_at)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
