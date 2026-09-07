-- V13: Todo rich text description
ALTER TABLE `todos`
  DROP COLUMN `description`,
  ADD COLUMN `description_json` JSON NULL,
  ADD COLUMN `description_plain_text` LONGTEXT NULL;
