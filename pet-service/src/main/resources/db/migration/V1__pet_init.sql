CREATE TABLE IF NOT EXISTS pet_role
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         VARCHAR(64) NOT NULL,
    pet_index       INT         NOT NULL,
    pet_id          INT         NOT NULL,
    level           INT         NOT NULL DEFAULT 1,
    exp             BIGINT      NOT NULL DEFAULT 0,
    pet_order       INT         NOT NULL DEFAULT 0,
    skill_lock_flag INT         NOT NULL DEFAULT 0,
    skills_json     JSON        NULL,
    gems_json       JSON        NULL,
    ts_gems_json    JSON        NULL,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_petindex (role_id, pet_index)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS pet_fight
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id            VARCHAR(64) NOT NULL UNIQUE,
    fight_indexes_json JSON        NULL,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;