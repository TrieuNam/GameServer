CREATE TABLE IF NOT EXISTS crafting_recipe (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    recipe_id       INT          NOT NULL,
    recipe_name     VARCHAR(255) NOT NULL,
    result_item_id  INT          NOT NULL,
    result_amount   INT          NOT NULL,
    materials_json  TEXT         NOT NULL,
    craft_time      BIGINT       NOT NULL,
    required_level  INT          NOT NULL,
    coin_cost       BIGINT       NULL,
    enabled         TINYINT      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipe_id (recipe_id),
    KEY idx_recipe_enabled (enabled),
    KEY idx_recipe_level (required_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_crafting (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_id     VARCHAR(64)  NOT NULL,
    recipe_id   INT          NOT NULL,
    start_time  TIMESTAMP(6) NOT NULL,
    end_time    TIMESTAMP(6) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    result_json TEXT         NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_user_crafting_role (role_id),
    KEY idx_user_crafting_status (status),
    KEY idx_user_crafting_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
