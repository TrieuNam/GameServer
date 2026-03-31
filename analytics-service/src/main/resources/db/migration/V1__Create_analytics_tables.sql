-- V1__Create_player_events_table.sql
CREATE TABLE IF NOT EXISTS player_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    event_data TEXT,
    event_time DATETIME NOT NULL,
    server_name VARCHAR(20),
    session_id VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_event (player_id, event_type),
    INDEX idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS player_kpi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    date DATETIME NOT NULL,
    login_count INT NOT NULL DEFAULT 0,
    session_duration INT NOT NULL DEFAULT 0,
    total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    purchase_count INT NOT NULL DEFAULT 0,
    battles_played INT NOT NULL DEFAULT 0,
    battles_won INT NOT NULL DEFAULT 0,
    pvp_matches INT NOT NULL DEFAULT 0,
    messagesent INT NOT NULL DEFAULT 0,
    friend_requests INT NOT NULL DEFAULT 0,
    tasks_completed INT NOT NULL DEFAULT 0,
    levels_gained INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_player_date (player_id, date),
    UNIQUE KEY uk_player_date (player_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
