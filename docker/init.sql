-- Tạo database (nếu chưa tồn tại)
create DATABASE IF NOT EXISTS db_user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS report_game_h2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database (nếu chưa tồn tại)
create DATABASE IF NOT EXISTS game_serverInfor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database (nếu chưa tồn tại)
create DATABASE IF NOT EXISTS session_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS task_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS db_equip_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS shizhuangdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS petDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create DATABASE IF NOT EXISTS db_item_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS globalserver_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS db_bag_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS db_box_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_cfg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_mail CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS gift_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_role CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_mainfb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS gameworld_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS globalserver_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS db_game_wallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user và phân quyền (nếu chưa tồn tại)
create user IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';

grant all privileges on db_user_service.* TO 'tpnam'@'%';
grant all privileges on report_game_h2.* TO 'tpnam'@'%';
grant all privileges on game_serverInfor.* TO 'tpnam'@'%';
grant all privileges on session_db.* TO 'tpnam'@'%';
grant all privileges on task_db.* TO 'tpnam'@'%';
grant all privileges on db_equip_service.* TO 'tpnam'@'%';
grant all privileges on shizhuangdb.* TO 'tpnam'@'%';
grant all privileges on petDB.* TO 'tpnam'@'%';
grant all privileges on db_item_service.* TO 'tpnam'@'%';
grant all privileges on globalserver_service_db.* TO 'tpnam'@'%';
grant all privileges on db_bag_service.* TO 'tpnam'@'%';
grant all privileges on db_box_service.* TO 'tpnam'@'%';
grant all privileges on game_shop.* TO 'tpnam'@'%';
grant all privileges on game_cfg.* TO 'tpnam'@'%';
grant all privileges on game_mail.* TO 'tpnam'@'%';
grant all privileges on gift_service.* TO 'tpnam'@'%';
grant all privileges on game_role.* TO 'tpnam'@'%';
grant all privileges on game_mainfb.* TO 'tpnam'@'%';
grant all privileges on gameworld_db.* TO 'tpnam'@'%';
grant all privileges on game_arena.* TO 'tpnam'@'%';
grant all privileges on db_game_wallet.* TO 'tpnam'@'%';

FLUSH PRIVILEGES;