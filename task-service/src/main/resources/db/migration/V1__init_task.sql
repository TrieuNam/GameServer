-- task-service consolidated schema (final: BIGINT player_id)

CREATE TABLE IF NOT EXISTS task_progress (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id      BIGINT      NOT NULL COMMENT 'Player identifier',
    task_key       VARCHAR(64) NOT NULL COMMENT 'Task unique key',
    progress_value INT         NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL COMMENT 'NOT_STARTED, IN_PROGRESS, COMPLETED, CLAIMED',
    last_update    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_player_task UNIQUE (player_id, task_key),
    INDEX idx_player     (player_id),
    INDEX idx_status     (status),
    INDEX idx_task_key   (task_key),
    INDEX idx_last_update (last_update)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS seven_day_sign (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id       BIGINT NOT NULL UNIQUE COMMENT 'Player identifier',
    start_epoch     BIGINT NOT NULL,
    signed_mask     INT    NOT NULL DEFAULT 0,
    claimed_mask    INT    NOT NULL DEFAULT 0,
    last_sign_date  DATE,
    INDEX idx_player        (player_id),
    INDEX idx_start_epoch   (start_epoch),
    INDEX idx_last_sign_date (last_sign_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
