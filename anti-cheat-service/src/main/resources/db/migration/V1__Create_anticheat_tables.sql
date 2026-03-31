-- Create cheat_reports table
CREATE TABLE cheat_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    cheat_type VARCHAR(50) NOT NULL COMMENT 'SPEED_HACK, TELEPORT, DAMAGE_HACK, RESOURCE_HACK, MEMORY_MODIFICATION, BOT',
    severity INT NOT NULL COMMENT '1=Low, 2=Medium, 3=High, 4=Critical',
    confidence DOUBLE NOT NULL COMMENT '0.0 - 1.0',
    evidence TEXT COMMENT 'JSON data',
    status VARCHAR(20) NOT NULL COMMENT 'DETECTED, REVIEWING, CONFIRMED, FALSE_POSITIVE, BANNED',
    action_taken VARCHAR(50) COMMENT 'WARNING, TEMPORARY_BAN, PERMANENT_BAN, NONE',
    reviewed_by VARCHAR(64) COMMENT 'GM ID',
    reviewed_at DATETIME,
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_cheat_type (cheat_type),
    INDEX idx_status (status),
    INDEX idx_severity (severity),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cheat detection reports';

-- Create player_behaviors table
CREATE TABLE player_behaviors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    metric_type VARCHAR(50) NOT NULL COMMENT 'MOVEMENT_SPEED, DAMAGE_DEALT, RESOURCE_GAINED, ACTION_RATE',
    metric_value DOUBLE NOT NULL,
    expected_value DOUBLE,
    deviation DOUBLE COMMENT 'Standard deviations from expected',
    is_anomaly BOOLEAN NOT NULL,
    context_data TEXT COMMENT 'JSON: position, level, equipment, etc.',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_metric_type (metric_type),
    INDEX idx_is_anomaly (is_anomaly),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Player behavior tracking';

-- Create suspicious_activities table
CREATE TABLE suspicious_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    suspicion_score INT NOT NULL COMMENT '0-100',
    description TEXT,
    details TEXT COMMENT 'JSON data',
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_suspicion_score (suspicion_score),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Suspicious activity tracking';
