-- V1: Complete 56 services configuration (all ports verified 2026-02-12)
-- Reference: docs/SERVICE-PORT-DB-MAPPING.md
-- Total: 56 services

CREATE TABLE IF NOT EXISTS admin_users (
    admin_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'GAME_MASTER',
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    permissions     JSON,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP    NULL,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_action_logs (
    log_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id         BIGINT       NOT NULL,
    action           VARCHAR(100) NOT NULL,
    target_player_id VARCHAR(50),
    details          JSON,
    ip_address       VARCHAR(50),
    timestamp        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_id      (admin_id),
    INDEX idx_timestamp     (timestamp DESC),
    INDEX idx_target_player (target_player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    startup_order INT NOT NULL,
    port INT NOT NULL,
    jar_path VARCHAR(500) NOT NULL,
    working_directory VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    requires_docker BOOLEAN NOT NULL DEFAULT FALSE,
    container_name VARCHAR(100),
    auto_start BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'STOPPED',
    process_id BIGINT,
    jvm_args VARCHAR(1000),
    app_args VARCHAR(1000),
    description VARCHAR(500),
    last_started TIMESTAMP NULL,
    last_stopped TIMESTAMP NULL,
    health_check_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phase (phase),
    INDEX idx_startup_order (startup_order),
    INDEX idx_enabled (enabled),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Service configurations with JVM args for Java 21 (Xss2m)';

-- ==========================================
-- P0: CORE INFRASTRUCTURE (4 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('eureka-server',   'Eureka Server',   'P0', 1, 8761, 'D:/project/serverGame/GameServer/eureka-server/target/eureka-server-1.0.0.jar',     'D:/project/serverGame/GameServer/eureka-server',   TRUE, FALSE, NULL, '-Xss2m -Xms128m -Xmx256m', 'Service Discovery and Registry',  'http://localhost:8761/actuator/health'),
('config-service',  'Config Service',  'P0', 2, 8888, 'D:/project/serverGame/GameServer/config-service/target/config-service-1.0.0.jar',   'D:/project/serverGame/GameServer/config-service',  TRUE, FALSE, NULL, '-Xss2m -Xms96m -Xmx192m',  'Centralized Configuration',       'http://localhost:8888/actuator/health'),
('gateway-service', 'Gateway Service', 'P0', 3, 8080, 'D:/project/serverGame/GameServer/gateway-service/target/gateway-service-1.0.0.jar', 'D:/project/serverGame/GameServer/gateway-service', TRUE, FALSE, NULL, '-Xss2m -Xms128m -Xmx256m', 'API Gateway',                     'http://localhost:8080/actuator/health'),
('webSocket-server','WebSocket Server','P0', 4, 8094, 'D:/project/serverGame/GameServer/webSocket-server/target/webSocket-server-1.0.0.jar','D:/project/serverGame/GameServer/webSocket-server',TRUE, FALSE, NULL, '-Xss2m -Xms128m -Xmx256m', 'Real-time Communication',         'http://localhost:8094/actuator/health');

-- ==========================================
-- P1: DATABASE + CORE GAMEPLAY (13 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('user-service',       'User Service',       'P1', 10, 8110, 'D:/project/serverGame/GameServer/user-service/target/user-service-1.0.0.jar',             'D:/project/serverGame/GameServer/user-service',       TRUE, TRUE,  'local-userdb',         '-Xss2m -Xms128m -Xmx256m', 'User Account Management',        'http://localhost:8110/actuator/health'),
('role-service',       'Role Service',       'P1', 11, 8410, 'D:/project/serverGame/GameServer/role-service/target/role-service-1.0.0.jar',             'D:/project/serverGame/GameServer/role-service',       TRUE, TRUE,  'local-roledb',         '-Xss2m -Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m', 'Character Roles',                'http://localhost:8410/actuator/health'),
('serverInfo-service', 'Server Info Service', 'P1', 12, 8095, 'D:/project/serverGame/GameServer/serverInfo-service/target/serverInfo-service-1.0.0.jar', 'D:/project/serverGame/GameServer/serverInfo-service', TRUE, TRUE,  'local-serverinfodb',   NULL,                        'Server Information',              'http://localhost:8095/actuator/health'),
('session-service',    'Session Service',    'P1', 13, 8096, 'D:/project/serverGame/GameServer/session-service/target/session-service-1.0.0.jar',       'D:/project/serverGame/GameServer/session-service',    TRUE, FALSE, NULL,                   '-Xss2m -Xms128m -Xmx256m', 'JWT Session Management',         'http://localhost:8096/actuator/health'),
('wallet-service',     'Wallet Service',     'P1', 14, 8210, 'D:/project/serverGame/GameServer/wallet-service/target/wallet-service-1.0.0.jar',         'D:/project/serverGame/GameServer/wallet-service',     TRUE, TRUE,  'local-walletdb',       '-Xss2m -Xms128m -Xmx256m', 'Currency and Wallet',            'http://localhost:8210/actuator/health'),
('report-service',     'Report Service',     'P1', 15, 8098, 'D:/project/serverGame/GameServer/report-service/target/report-service-1.0.0.jar','D:/project/serverGame/GameServer/report-service',     TRUE, TRUE,  'local-reportdb',       NULL,                        'Analytics and Reporting',        'http://localhost:8098/actuator/health'),
('iap-verify-service', 'IAP Verify Service', 'P1', 16, 8580, 'D:/project/serverGame/GameServer/iap-verify-service/target/iap-verify-service-1.0.0.jar', 'D:/project/serverGame/GameServer/iap-verify-service', TRUE, TRUE,  'local-iapverifydb',    '-Xss2m -Xms96m -Xmx192m',  'In-App Purchase Verification',   'http://localhost:8580/actuator/health'),
('bag-service',        'Bag Service',        'P1', 17, 8230, 'D:/project/serverGame/GameServer/bag-service/target/bag-service-1.0.0.jar',               'D:/project/serverGame/GameServer/bag-service',        TRUE, TRUE,  'local-bagdb',          '-Xss2m -Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m', 'Inventory Management',           'http://localhost:8230/actuator/health'),
('equip-service',      'Equip Service',      'P1', 18, 8240, 'D:/project/serverGame/GameServer/equip-service/target/equip-service-1.0.0.jar',           'D:/project/serverGame/GameServer/equip-service',      TRUE, TRUE,  'local-equipdb',        NULL,                        'Equipment System',               'http://localhost:8240/actuator/health'),
('drop-service',       'Drop Service',       'P1', 19, 8250, 'D:/project/serverGame/GameServer/drop-service/target/drop-service-1.0.0.jar',             'D:/project/serverGame/GameServer/drop-service',       TRUE, FALSE, NULL,                   '-Xss2m -Xms96m -Xmx192m',  'Loot and Rewards (No DB)',       'http://localhost:8250/actuator/health'),
('shop-service',       'Shop Service',       'P1', 20, 8260, 'D:/project/serverGame/GameServer/shop-service/target/shop-service-1.0.0.jar',             'D:/project/serverGame/GameServer/shop-service',       TRUE, TRUE,  'local-shopdb',         '-Xss2m -Xms96m -Xmx192m',  'In-game Store',                  'http://localhost:8260/actuator/health'),
('gift-service',       'Gift Service',       'P1', 21, 8270, 'D:/project/serverGame/GameServer/gift-service/target/gift-service-1.0.0.jar',             'D:/project/serverGame/GameServer/gift-service',       TRUE, FALSE, NULL,                   '-Xss2m -Xms96m -Xmx192m',  'Gift Code Redemption (No DB)',   'http://localhost:8270/actuator/health'),
('box-service',        'Box Service',        'P1', 22, 8290, 'D:/project/serverGame/GameServer/box-service/target/box-service-1.0.0.jar',               'D:/project/serverGame/GameServer/box-service',        TRUE, TRUE,  'local-boxdb',          NULL,                        'Mystery Box System',             'http://localhost:8290/actuator/health');

-- ==========================================
-- P2: COMBAT & WORLD & SOCIAL (13 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('crafting-service',      'Crafting Service',      'P2', 30, 8280, 'D:/project/serverGame/GameServer/crafting-service/target/crafting-service-1.0.0.jar',           'D:/project/serverGame/GameServer/crafting-service',      TRUE, TRUE,  'local-craftingdb',       '-Xss2m -Xms64m -Xmx128m', 'Item Crafting',              'http://localhost:8280/actuator/health'),
('arena-service',         'Arena Service',         'P2', 31, 8084, 'D:/project/serverGame/GameServer/arena-service/target/arena-service-1.0.0.jar',               'D:/project/serverGame/GameServer/arena-service',         TRUE, TRUE,  'local-arenadb',          '-Xss2m -Xms64m -Xmx128m', 'PvP Arena System',           'http://localhost:8084/actuator/health'),
('trial-service',         'Trial Service',         'P2', 32, 8300, 'D:/project/serverGame/GameServer/trial-service/target/trial-service-1.0.0.jar',               'D:/project/serverGame/GameServer/trial-service',         TRUE, TRUE,  'local-trialdb',          '-Xss2m -Xms64m -Xmx128m', 'Challenge Trials',           'http://localhost:8300/actuator/health'),
('task-service',          'Task Service',          'P2', 33, 8097, 'D:/project/serverGame/GameServer/task-service/target/task-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/task-service',          TRUE, TRUE,  'local-taskdb',           '-Xss2m -Xms64m -Xmx128m', 'Quest and Task System',      'http://localhost:8097/actuator/health'),
('battleserver-service',  'Battle Server',         'P2', 34, 8082, 'D:/project/serverGame/GameServer/battleserver-service/target/battleserver-service-1.0.0.jar', 'D:/project/serverGame/GameServer/battleserver-service',  TRUE, TRUE,  'local-battleserverdb',   '-Xss2m -Xms64m -Xmx128m', 'Combat Processing',          'http://localhost:8082/actuator/health'),
('globalserver-service',  'Global Server',         'P2', 35, 8100, 'D:/project/serverGame/GameServer/globalserver-service/target/globalserver-service-1.0.0.jar', 'D:/project/serverGame/GameServer/globalserver-service',  TRUE, TRUE,  'local-globalserverdb',   '-Xss2m -Xms64m -Xmx128m', 'Cross-server Features',      'http://localhost:8100/actuator/health'),
('gameworld-service',     'Game World Service',    'P2', 36, 8105, 'D:/project/serverGame/GameServer/gameworld-service/target/gameworld-service-1.0.0.jar',       'D:/project/serverGame/GameServer/gameworld-service',     TRUE, TRUE,  'local-gameworlddb',      '-Xss2m -Xms64m -Xmx128m', 'Player Position Tracking',   'http://localhost:8105/actuator/health'),
('starmap-service',       'Starmap Service',       'P2', 37, 8092, 'D:/project/serverGame/GameServer/starmap-service/target/starmap-service-1.0.0.jar',           'D:/project/serverGame/GameServer/starmap-service',       TRUE, TRUE,  'local-starmapdb',        '-Xss2m -Xms64m -Xmx128m', 'Constellation System',       'http://localhost:8092/actuator/health'),
('territory-service',     'Territory Service',     'P2', 38, 8360, 'D:/project/serverGame/GameServer/territory-service/target/territory-service-1.0.0.jar',       'D:/project/serverGame/GameServer/territory-service',     TRUE, TRUE,  'local-territorydb',      '-Xss2m -Xms64m -Xmx128m', 'Territory Control',          'http://localhost:8360/actuator/health'),
('escort-service',        'Escort Service',        'P2', 39, 8340, 'D:/project/serverGame/GameServer/escort-service/target/escort-service-1.0.0.jar',             'D:/project/serverGame/GameServer/escort-service',        TRUE, TRUE,  'local-escortdb',         '-Xss2m -Xms64m -Xmx128m', 'Escort Missions',            'http://localhost:8340/actuator/health'),
('world-service',         'World Service',         'P2', 40, 8370, 'D:/project/serverGame/GameServer/world-service/target/world-service-1.0.0.jar',               'D:/project/serverGame/GameServer/world-service',         TRUE, TRUE,  'local-worlddb',          '-Xss2m -Xms64m -Xmx128m', 'World State Management',     'http://localhost:8370/actuator/health'),
('chat-service',          'Chat Service',          'P2', 41, 8460, 'D:/project/serverGame/GameServer/chat-service/target/chat-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/chat-service',          TRUE, TRUE,  'local-chatdb',           '-Xss2m -Xms64m -Xmx128m', 'Chat System',                'http://localhost:8460/actuator/health'),
('guild-service',         'Guild Service',         'P2', 42, 8440, 'D:/project/serverGame/GameServer/guild-service/target/guild-service-1.0.0.jar',               'D:/project/serverGame/GameServer/guild-service',         TRUE, TRUE,  'local-guilddb',          '-Xss2m -Xms64m -Xmx128m', 'Guild Management',           'http://localhost:8440/actuator/health');

-- ==========================================
-- P3: ENHANCEMENT FEATURES & SUPPORT (16 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('friend-service',        'Friend Service',        'P3', 50, 8450, 'D:/project/serverGame/GameServer/friend-service/target/friend-service-1.0.0.jar',             'D:/project/serverGame/GameServer/friend-service',        TRUE, TRUE,  'local-frienddb',         '-Xss2m -Xms64m -Xmx128m',  'Friends and Social',          'http://localhost:8450/actuator/health'),
('mail-service',          'Mail Service',          'P3', 51, 8470, 'D:/project/serverGame/GameServer/mail-service/target/mail-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/mail-service',          TRUE, TRUE,  'local-maildb',           '-Xss2m -Xms64m -Xmx128m',  'In-game Mail System',         'http://localhost:8470/actuator/health'),
('leaderboard-service',   'Leaderboard Service',   'P3', 52, 8480, 'D:/project/serverGame/GameServer/leaderboard-service/target/leaderboard-service-1.0.0.jar',   'D:/project/serverGame/GameServer/leaderboard-service',   TRUE, TRUE,  'local-leaderboarddb',    '-Xss2m -Xms64m -Xmx128m',  'Rankings and Leaderboards',   'http://localhost:8480/actuator/health'),
('pet-service',           'Pet Service',           'P3', 53, 8112, 'D:/project/serverGame/GameServer/pet-service/target/pet-service-1.0.0.jar',                   'D:/project/serverGame/GameServer/pet-service',           TRUE, TRUE,  'local-petdb',            '-Xss2m -Xms64m -Xmx128m',  'Pet System',                  'http://localhost:8112/actuator/health'),
('mount-service',         'Mount Service',         'P3', 54, 8089, 'D:/project/serverGame/GameServer/mount-service/target/mount-service-1.0.0.jar',               'D:/project/serverGame/GameServer/mount-service',         TRUE, TRUE,  'local-mountdb',          '-Xss2m -Xms64m -Xmx128m',  'Mount System',                'http://localhost:8089/actuator/health'),
('rune-service',          'Rune Service',          'P3', 55, 8093, 'D:/project/serverGame/GameServer/rune-service/target/rune-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/rune-service',          TRUE, TRUE,  'local-runedb',           '-Xss2m -Xms64m -Xmx128m',  'Rune Enhancement',            'http://localhost:8093/actuator/health'),
('item-service',          'Item Service',          'P3', 56, 8220, 'D:/project/serverGame/GameServer/item-service/target/item-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/item-service',          TRUE, FALSE, NULL,                     '-Xss2m -Xms96m -Xmx192m',  'Item Templates (Cache only)', 'http://localhost:8220/actuator/health'),
('shizhuang-service',     'Shizhuang Service',     'P3', 57, 8350, 'D:/project/serverGame/GameServer/shizhuang-service/target/shizhuang-service-1.0.0.jar',       'D:/project/serverGame/GameServer/shizhuang-service',     TRUE, TRUE,  'local-shizhuangdb',      '-Xss2m -Xms64m -Xmx128m',  'Fashion System',              'http://localhost:8350/actuator/health'),
('angel-service',         'Angel Service',         'P3', 58, 8090, 'D:/project/serverGame/GameServer/angel-service/target/angel-service-1.0.0.jar',               'D:/project/serverGame/GameServer/angel-service',         TRUE, TRUE,  'local-angeldb',          '-Xss2m -Xms64m -Xmx128m',  'Guardian Angel System',       'http://localhost:8090/actuator/health'),
('artifact-service',      'Artifact Service',      'P3', 59, 8091, 'D:/project/serverGame/GameServer/artifact-service/target/artifact-service-1.0.0.jar',         'D:/project/serverGame/GameServer/artifact-service',      TRUE, TRUE,  'local-artifactdb',       '-Xss2m -Xms64m -Xmx128m',  'Artifact System',             'http://localhost:8091/actuator/health'),
('analytics-service',     'Analytics Service',     'P3', 60, 8510, 'D:/project/serverGame/GameServer/analytics-service/target/analytics-service-1.0.0.jar',       'D:/project/serverGame/GameServer/analytics-service',     TRUE, TRUE,  'local-analyticsdb',      '-Xss2m -Xms48m -Xmx96m',   'Player Analytics',            'http://localhost:8510/actuator/health'),
('notification-service',  'Notification Service',  'P3', 61, 8520, 'D:/project/serverGame/GameServer/notification-service/target/notification-service-1.0.0.jar', 'D:/project/serverGame/GameServer/notification-service',  TRUE, TRUE,  'local-notificationdb',   '-Xss2m -Xms64m -Xmx128m',  'Push Notifications',          'http://localhost:8520/actuator/health'),
('moderation-service',    'Moderation Service',    'P3', 62, 8570, 'D:/project/serverGame/GameServer/moderation-service/target/moderation-service-1.0.0.jar',     'D:/project/serverGame/GameServer/moderation-service',    TRUE, TRUE,  'local-moderationdb',     '-Xss2m -Xms48m -Xmx96m',   'Content Moderation',          'http://localhost:8570/actuator/health'),
('file-service',          'File Service',          'P3', 63, 8540, 'D:/project/serverGame/GameServer/file-service/target/file-service-1.0.0.jar',                 'D:/project/serverGame/GameServer/file-service',          TRUE, FALSE, NULL,                     '-Xss2m -Xms48m -Xmx96m',   'File Management',             'http://localhost:8540/actuator/health'),
('scheduler-service',     'Scheduler Service',     'P3', 64, 8550, 'D:/project/serverGame/GameServer/scheduler-service/target/scheduler-service-1.0.0.jar',       'D:/project/serverGame/GameServer/scheduler-service',     TRUE, FALSE, NULL,                     '-Xss2m -Xms48m -Xmx96m',   'Task Scheduler (Redis)',      'http://localhost:8550/actuator/health'),
('localization-service',  'Localization Service',  'P3', 65, 8560, 'D:/project/serverGame/GameServer/localization-service/target/localization-service-1.0.0.jar', 'D:/project/serverGame/GameServer/localization-service',  TRUE, FALSE, NULL,                     '-Xss2m -Xms48m -Xmx96m',   'Multi-language Support',      'http://localhost:8560/actuator/health');

-- ==========================================
-- P3 (CONT): NEW GAME FEATURES (6 services) -- Added 2026-02-27
-- NOTE: lingzhu=8380 (was 8300, conflict trial), gem=8381 (was 8340, conflict escort), activity=8382 (was 8350, conflict shizhuang)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('lingzhu-service',  'LingZhu Service',   'P3', 66, 8380, 'D:/project/serverGame/GameServer/lingzhu-service/target/lingzhu-service-1.0.0.jar',   'D:/project/serverGame/GameServer/lingzhu-service',  TRUE, TRUE,  'local-lingzhudb',   '-Xss2m -Xms64m -Xmx128m', 'Lord Dungeon (领主副本)',         'http://localhost:8380/actuator/health'),
('knights-service',  'Knights Service',   'P3', 67, 8310, 'D:/project/serverGame/GameServer/knights-service/target/knights-service-1.0.0.jar',   'D:/project/serverGame/GameServer/knights-service',  TRUE, TRUE,  'local-knightsdb',   '-Xss2m -Xms64m -Xmx128m', 'Knights Handbook (骑士手册)',     'http://localhost:8310/actuator/health'),
('pagoda-service',   'Pagoda Service',    'P3', 68, 8320, 'D:/project/serverGame/GameServer/pagoda-service/target/pagoda-service-1.0.0.jar',     'D:/project/serverGame/GameServer/pagoda-service',   TRUE, TRUE,  'local-pagodadb',    '-Xss2m -Xms64m -Xmx128m', 'Tower Systems (试炼塔/锢魔塔)',   'http://localhost:8320/actuator/health'),
('scroll-service',   'Scroll Service',    'P3', 69, 8330, 'D:/project/serverGame/GameServer/scroll-service/target/scroll-service-1.0.0.jar',     'D:/project/serverGame/GameServer/scroll-service',   TRUE, TRUE,  'local-scrolldb',    '-Xss2m -Xms64m -Xmx128m', 'Scroll Lottery (卷轴)',           'http://localhost:8330/actuator/health'),
('gem-service',      'Gem Service',       'P3', 70, 8381, 'D:/project/serverGame/GameServer/gem-service/target/gem-service-1.0.0.jar',           'D:/project/serverGame/GameServer/gem-service',      TRUE, TRUE,  'local-gemdb',       '-Xss2m -Xms64m -Xmx128m', 'Gem Drawings (宝石图纸)',         'http://localhost:8381/actuator/health'),
('activity-service', 'Activity Service',  'P3', 71, 8382, 'D:/project/serverGame/GameServer/activity-service/target/activity-service-1.0.0.jar', 'D:/project/serverGame/GameServer/activity-service', TRUE, TRUE,  'local-activitydb',  '-Xss2m -Xms64m -Xmx128m', 'New Server Activities (新服活动)', 'http://localhost:8382/actuator/health');

-- ==========================================
-- P4: OPTIONAL FEATURES (2 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('main-fb-service',    'Main FB Service',    'P4', 80, 8128, 'D:/project/serverGame/GameServer/main-fb-service/target/main-fb-service-1.0.0.jar',       'D:/project/serverGame/GameServer/main-fb-service',    TRUE, TRUE,  'local-mainfbdb',    '-Xss2m -Xms64m -Xmx128m', 'Facebook Integration',    'http://localhost:8128/actuator/health'),
('anti-cheat-service', 'Anti-Cheat Service', 'P4', 81, 8590, 'D:/project/serverGame/GameServer/anti-cheat-service/target/anti-cheat-service-1.0.0.jar', 'D:/project/serverGame/GameServer/anti-cheat-service', TRUE, TRUE,  'local-anticheatdb', '-Xss2m -Xms64m -Xmx128m', 'Anti-Cheat Detection',    'http://localhost:8590/actuator/health');

-- ==========================================
-- SPECIAL: ADMIN & SUPPORT (2 services)
-- ==========================================
INSERT INTO service_config (service_name, display_name, phase, startup_order, port, jar_path, working_directory, enabled, requires_docker, container_name, jvm_args, description, health_check_url) VALUES
('admin-service', 'Admin Service', 'SPECIAL', 90, 9091, 'D:/project/serverGame/GameServer/admin-service/target/admin-service-1.0.0.jar', 'D:/project/serverGame/GameServer/admin-service', TRUE, TRUE,  'local-admindb', '-Xss2m -Xms128m -Xmx256m', 'Admin Control Panel', 'http://localhost:9091/actuator/health'),
('gm-service',    'GM Service',    'SPECIAL', 91, 9093, 'D:/project/serverGame/GameServer/gm-service/target/gm-service-1.0.0.jar',       'D:/project/serverGame/GameServer/gm-service',    TRUE, TRUE,  'local-gmdb',    '-Xss2m -Xms64m -Xmx128m',  'Game Master Tools',   'http://localhost:9093/actuator/health');

-- ==========================================
-- Total: 56 services
-- P0: 4  (Infrastructure)
-- P1: 13 (Database + Core Gameplay)
-- P2: 13 (Combat + World + Social)
-- P3: 22 (Enhancement Features + Support + New Game Features)
-- P4: 2  (Optional)
-- SPECIAL: 2 (Admin)
-- ==========================================
