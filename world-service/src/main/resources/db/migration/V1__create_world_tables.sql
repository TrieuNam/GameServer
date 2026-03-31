-- World Service Database Schema
-- Version: 1.0
-- Description: Tables for managing world state, events, and bosses

-- Table: world_state
-- Purpose: Store global and server-specific state information
CREATE TABLE world_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    state_key VARCHAR(50) NOT NULL UNIQUE COMMENT 'State identifier (e.g., GLOBAL_STATE, SERVER_1_STATE)',
    state_data JSON COMMENT 'JSON object containing state information',
    version INT NOT NULL DEFAULT 1 COMMENT 'State version for optimistic locking',
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Record creation time',
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Last update time',
    INDEX idx_state_key (state_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores world state information for global and server-specific data';

-- Table: world_events
-- Purpose: Manage world-wide events (boss spawns, double exp, festivals, etc.)
CREATE TABLE world_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL COMMENT 'Display name of the event',
    event_type VARCHAR(50) NOT NULL COMMENT 'Type: WORLD_BOSS, DOUBLE_EXP, DOUBLE_DROP, SPECIAL_SHOP, SIEGE_WAR, FESTIVAL, MAINTENANCE, CUSTOM',
    description TEXT COMMENT 'Detailed description of the event',
    event_data JSON COMMENT 'JSON configuration for event parameters',
    start_time TIMESTAMP(6) NOT NULL COMMENT 'Event start time',
    end_time TIMESTAMP(6) NOT NULL COMMENT 'Event end time',
    active BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether event is currently active',
    recurring BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether event repeats',
    cron_schedule VARCHAR(50) COMMENT 'Cron expression for recurring events',
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Record creation time',
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Last update time',
    INDEX idx_event_type (event_type),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Manages world-wide events and their schedules';

-- Table: world_bosses
-- Purpose: Track world boss instances, status, HP, and rewards
CREATE TABLE world_bosses (
    boss_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    boss_name VARCHAR(100) NOT NULL COMMENT 'Name of the world boss',
    boss_level INT NOT NULL COMMENT 'Boss level/difficulty',
    current_hp BIGINT NOT NULL DEFAULT 0 COMMENT 'Current health points',
    max_hp BIGINT NOT NULL DEFAULT 1000000 COMMENT 'Maximum health points',
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_SPAWNED' COMMENT 'Status: NOT_SPAWNED, SPAWNING, ACTIVE, DEFEATED, DESPAWNED',
    location VARCHAR(100) COMMENT 'Map/zone where boss spawns',
    spawn_time TIMESTAMP(6) COMMENT 'When boss spawned',
    defeated_time TIMESTAMP(6) COMMENT 'When boss was defeated',
    defeated_by VARCHAR(50) COMMENT 'Player name or guild that dealt final blow',
    rewards JSON COMMENT 'JSON configuration for boss rewards',
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Record creation time',
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Last update time',
    INDEX idx_boss_status (status),
    INDEX idx_spawn_time (spawn_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks world boss instances and their current state';

-- Insert sample data for testing

-- Sample world state
INSERT INTO world_state (state_key, state_data, version) VALUES
('GLOBAL_STATE', '{"serverStatus": "ONLINE", "playerCount": 0, "maintenanceMode": false}', 1),
('SERVER_1_STATE', '{"name": "Server 1", "capacity": 10000, "currentPlayers": 0}', 1);

-- Sample world events
INSERT INTO world_events (event_name, event_type, description, event_data, start_time, end_time, active, recurring) VALUES
('Weekend Double EXP', 'DOUBLE_EXP', 'Double experience points for all activities', '{"expMultiplier": 2.0}', NOW(), DATE_ADD(NOW(), INTERVAL 48 HOUR), TRUE, FALSE),
('Dragon King Spawn', 'WORLD_BOSS', 'Legendary Dragon King appears in Dragon Valley', '{"bossId": 1001, "rewards": ["legendary_weapon", "rare_gems"]}', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), TRUE, FALSE);

-- Sample world boss
INSERT INTO world_bosses (boss_name, boss_level, current_hp, max_hp, status, location, spawn_time) VALUES
('Dragon King', 100, 5000000, 5000000, 'ACTIVE', 'Dragon Valley - Central Peak', NOW()),
('Ice Giant', 80, 3000000, 3000000, 'NOT_SPAWNED', 'Frozen Wasteland', NULL);
