-- Fix enabled column to use TINYINT without (1) to prevent JDBC from mapping it to BIT
ALTER TABLE crafting_recipe MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 1;
