-- GM Service Database Schema
-- V1__Create_gm_action_logs_table.sql

CREATE TABLE IF NOT EXISTS gm_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gm_id BIGINT NOT NULL COMMENT 'GM user ID',
    gm_username VARCHAR(50) NOT NULL COMMENT 'GM username',
    action VARCHAR(100) NOT NULL COMMENT 'Action type: GIVE_ITEM, ADD_CURRENCY, BAN_USER, etc',
    target_player_id VARCHAR(50) COMMENT 'Target player ID',
    target_user_id BIGINT COMMENT 'Target user ID',
    details TEXT COMMENT 'Action details in JSON format',
    reason VARCHAR(255) COMMENT 'Reason for the action',
    ip_address VARCHAR(50) COMMENT 'GM IP address',
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Action timestamp',
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS or FAILED',
    error_message TEXT COMMENT 'Error message if failed',
    
    INDEX idx_gm_id (gm_id),
    INDEX idx_target_player_id (target_player_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='GM action audit logs';
