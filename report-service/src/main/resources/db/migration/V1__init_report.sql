-- V1__init_report.sql
-- Initial schema for report-service (DB: report_db, Port: 3309)

-- =====================================================================
-- Table: report_event
-- Purpose: Store game analytics events from clients
-- =====================================================================
CREATE TABLE IF NOT EXISTS report_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type INT COMMENT 'Event type code',
    agent_id VARCHAR(100) COMMENT 'Agent/channel ID',
    device_id VARCHAR(255) COMMENT 'Device unique ID',
    package_version VARCHAR(50) COMMENT 'Client package version',
    source_version VARCHAR(50) COMMENT 'Source code version',
    session_id VARCHAR(255) COMMENT 'Session identifier',
    login_time BIGINT COMMENT 'Login timestamp (millis)',
    net_state INT COMMENT 'Network state (wifi/mobile)',
    event_time BIGINT COMMENT 'Event timestamp from client (millis)',
    imea VARCHAR(255) COMMENT 'IMEI/device hardware ID',
    channel_id VARCHAR(100) COMMENT 'Channel/distribution ID',
    extra_params VARCHAR(2000) COMMENT 'Extended parameters (JSON or key-value)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'DB insertion time',
    
    INDEX idx_type (type),
    INDEX idx_agent_id (agent_id),
    INDEX idx_device_id (device_id),
    INDEX idx_session_id (session_id),
    INDEX idx_event_time (event_time),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Game analytics events';

-- =====================================================================
-- Table: notice
-- Purpose: System notices/notifications sent to clients
-- =====================================================================
CREATE TABLE IF NOT EXISTS notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type INT NOT NULL COMMENT '0:System, 1:ItemNotEnough, 2:SystemMsg, 3:ZeroHour, 4:RechargeRet',
    content VARCHAR(1024) COMMENT 'Notice content/message',
    code INT COMMENT 'Error/status code',
    item_id INT COMMENT 'Item ID (for ItemNotEnough type)',
    created_time BIGINT NOT NULL COMMENT 'Creation timestamp (millis)',
    
    INDEX idx_type (type),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='System notices and notifications';

-- =====================================================================
-- Table: user_boss_kill
-- Purpose: Track boss kill counts per user
-- =====================================================================
CREATE TABLE IF NOT EXISTS user_boss_kill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT 'User ID',
    boss_kill_count INT NOT NULL DEFAULT 0 COMMENT 'Total boss kills',
    
    INDEX idx_user_id (user_id),
    INDEX idx_boss_kill_count (boss_kill_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Boss kill statistics per user';
