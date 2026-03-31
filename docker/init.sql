-- ============================================================
-- GameServer — MySQL Database Initialization
-- Tạo tất cả databases cho tất cả services
-- Cập nhật: 2026-03-14  |  Format chuẩn: game_<name>
-- ============================================================

-- ── P1: Core Gameplay ────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS `game_user`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_role`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_serverinfo`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_wallet`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_report`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_iap_verify`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_bag`         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_equip`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_shop`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_box`         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_crafting`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── P2: Combat, World & Social ───────────────────────────────
CREATE DATABASE IF NOT EXISTS `game_arena`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_trial`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_task`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_battle`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_globalserver` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_gameworld`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_starmap`     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_territory`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_escort`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_world`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_chat`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_guild`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── P3: Enhancement & Support ────────────────────────────────
CREATE DATABASE IF NOT EXISTS `game_friend`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_mail`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_leaderboard` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_pet`         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_mount`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_rune`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_angel`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_artifact`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_analytics`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_notification` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_moderation`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_shizhuang`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_lingzhu`     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_knights`     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_pagoda`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_scroll`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_gem`         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_activity`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── P4: Optional Features ────────────────────────────────────
CREATE DATABASE IF NOT EXISTS `game_mainfb`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_anticheat`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── Special: Admin & GM ──────────────────────────────────────
CREATE DATABASE IF NOT EXISTS `game_admin`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `game_gm`          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── Tạo user và cấp quyền ────────────────────────────────────
CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';

GRANT ALL PRIVILEGES ON `game_user`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_role`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_serverinfo`.*  TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_wallet`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_report`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_iap_verify`.*  TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_bag`.*         TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_equip`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_shop`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_box`.*         TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_crafting`.*    TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_arena`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_trial`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_task`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_battle`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_globalserver`.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_gameworld`.*   TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_starmap`.*     TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_territory`.*   TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_escort`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_world`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_chat`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_guild`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_friend`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_mail`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_leaderboard`.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_pet`.*         TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_mount`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_rune`.*        TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_angel`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_artifact`.*    TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_analytics`.*   TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_notification`.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_moderation`.*  TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_shizhuang`.*   TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_lingzhu`.*     TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_knights`.*     TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_pagoda`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_scroll`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_gem`.*         TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_activity`.*    TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_mainfb`.*      TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_anticheat`.*   TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_admin`.*       TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON `game_gm`.*          TO 'tpnam'@'%';

FLUSH PRIVILEGES;
