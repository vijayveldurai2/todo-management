-- V8: Wire Todo to Status and Sprint, remove direct column/board reference, drop completed/completed_date

-- 1. Drop old foreign keys and columns from todos
ALTER TABLE `todos`
  DROP FOREIGN KEY `fk_todos_column`,
  DROP FOREIGN KEY `fk_v5_todos_board`,
  DROP COLUMN `column_id`,
  DROP COLUMN `board_id`,
  DROP COLUMN `completed`,
  DROP COLUMN `completed_date`;

-- 2. Add status_id (NOT NULL, FK to statuses) and sprint_id (nullable, FK to sprint_boards)
ALTER TABLE `todos`
  ADD COLUMN `status_id` char(36) NOT NULL AFTER `priority`,
  ADD COLUMN `sprint_id` char(36) DEFAULT NULL AFTER `status_id`,
  ADD KEY `fk_todos_status` (`status_id`),
  ADD KEY `fk_todos_sprint` (`sprint_id`),
  ADD CONSTRAINT `fk_todos_status` FOREIGN KEY (`status_id`) REFERENCES `statuses` (`id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_todos_sprint` FOREIGN KEY (`sprint_id`) REFERENCES `sprint_boards` (`id`) ON DELETE SET NULL;
