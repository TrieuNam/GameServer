ALTER TABLE `role`
    ADD COLUMN `notice_time` BIGINT NOT NULL DEFAULT 0 AFTER `guild_name`;