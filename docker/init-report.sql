CREATE DATABASE IF NOT EXISTS report_game_h2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user và phân quyền (nếu chưa tồn tại)
CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';

GRANT ALL PRIVILEGES ON report_game_h2.* TO 'tpnam'@'%';

FLUSH PRIVILEGES;