ALTER TABLE arena_players
    ADD COLUMN challenges_used_today INT NOT NULL DEFAULT 0 AFTER last_battle_time,
    ADD COLUMN last_reset_date DATE NULL AFTER challenges_used_today;
