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

FLUSH PRIVILEGES;