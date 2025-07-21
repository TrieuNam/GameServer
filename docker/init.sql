-- Tạo database (nếu chưa tồn tại)
CREATE DATABASE IF NOT EXISTS db_user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS report_game_h2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database (nếu chưa tồn tại)
CREATE DATABASE IF NOT EXISTS game_serverInfor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database (nếu chưa tồn tại)
CREATE DATABASE IF NOT EXISTS session_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user và phân quyền (nếu chưa tồn tại)
CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';

GRANT ALL PRIVILEGES ON db_user_service.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON report_game_h2.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON game_serverInfor.* TO 'tpnam'@'%';
GRANT ALL PRIVILEGES ON session_db.* TO 'tpnam'@'%';

FLUSH PRIVILEGES;