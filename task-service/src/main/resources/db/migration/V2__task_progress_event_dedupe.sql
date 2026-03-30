CREATE TABLE IF NOT EXISTS task_progress_event (
    event_id     VARCHAR(64) NOT NULL PRIMARY KEY,
    player_id    BIGINT      NOT NULL COMMENT 'Player identifier',
    task_key     VARCHAR(64) NOT NULL COMMENT 'Task key applied by this event',
    source       VARCHAR(64) NULL COMMENT 'Producer service/source',
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_player_id (player_id),
    INDEX idx_task_key (task_key),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;