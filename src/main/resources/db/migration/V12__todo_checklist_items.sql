-- V12: Checklist items for todos
CREATE TABLE `checklist_items` (
  `id` char(36) NOT NULL,
  `todo_id` char(36) NOT NULL,
  `text` text NOT NULL,
  `is_checked` tinyint(1) NOT NULL DEFAULT 0,
  `position` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_checklist_items_todo` (`todo_id`),
  KEY `idx_checklist_items_todo_pos` (`todo_id`, `position`),
  CONSTRAINT `fk_checklist_items_todo` FOREIGN KEY (`todo_id`) REFERENCES `todos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
