ALTER TABLE bag_items
    ADD COLUMN quality INT NOT NULL DEFAULT 1 AFTER bind,
    ADD COLUMN bag_type TINYINT NOT NULL DEFAULT 0 AFTER quality;

ALTER TABLE bag_items
    DROP INDEX uk_role_item_bind_exp,
    ADD UNIQUE KEY uk_role_item_bind_exp_quality_type (role_id, item_id, bind, expire_at, quality, bag_type);

