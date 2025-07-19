-- Tạo database (nếu chưa tồn tại)
CREATE DATABASE IF NOT EXISTS db_user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user và phân quyền (nếu chưa tồn tại)
CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';

GRANT ALL PRIVILEGES ON db_user_service.* TO 'tpnam'@'%';

FLUSH PRIVILEGES;