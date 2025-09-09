-- Tạo database (nếu chưa tồn tại)
create DATABASE IF NOT EXISTS userdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS report_game_h2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
create DATABASE IF NOT EXISTS game_role CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;



-- Tạo user và phân quyền (nếu chưa tồn tại)
create user IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';
grant all privileges on userdb.* TO 'tpnam'@'%';
grant all privileges on report_game_h2.* TO 'tpnam'@'%';
grant all privileges on game_role.* TO 'tpnam'@'%';


FLUSH PRIVILEGES;