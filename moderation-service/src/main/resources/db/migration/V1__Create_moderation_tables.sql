CREATE TABLE IF NOT EXISTS reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id VARCHAR(64) NOT NULL,
    reported_user_id VARCHAR(64) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    content TEXT,
    evidence TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution TEXT,
    handled_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP,
    INDEX idx_reported_user (reported_user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS violations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    content TEXT,
    severity INT NOT NULL DEFAULT 1,
    action_taken VARCHAR(50),
    duration_hours INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
