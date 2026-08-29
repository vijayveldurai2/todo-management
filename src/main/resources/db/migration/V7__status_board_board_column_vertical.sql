-- V7: Status, Board (JOINED inheritance), BoardColumn, and Additional Statuses

-- 1. Statuses table
CREATE TABLE `statuses` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `category` varchar(30) NOT NULL,
  `position` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statuses_project_name` (`project_id`, `name`),
  KEY `fk_statuses_project` (`project_id`),
  CONSTRAINT `fk_statuses_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Boards table adjustment for JPA Joined Inheritance
ALTER TABLE `boards`
  ADD COLUMN `board_type` varchar(30) NOT NULL DEFAULT 'KANBAN' AFTER `name`,
  ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3. Kanban Boards table
CREATE TABLE `kanban_boards` (
  `id` char(36) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_kanban_boards_board` FOREIGN KEY (`id`) REFERENCES `boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Sprint Boards table
CREATE TABLE `sprint_boards` (
  `id` char(36) NOT NULL,
  `sprint_start_date` date DEFAULT NULL,
  `sprint_end_date` date DEFAULT NULL,
  `sprint_goal` varchar(1000) DEFAULT NULL,
  `sprint_status` varchar(30) NOT NULL DEFAULT 'PLANNING',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_sprint_boards_board` FOREIGN KEY (`id`) REFERENCES `boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Board Columns update with primary_status_id
ALTER TABLE `board_columns`
  ADD COLUMN `primary_status_id` char(36) NOT NULL AFTER `position`,
  ADD KEY `fk_board_columns_primary_status` (`primary_status_id`),
  ADD CONSTRAINT `fk_board_columns_primary_status` FOREIGN KEY (`primary_status_id`) REFERENCES `statuses` (`id`) ON DELETE RESTRICT;

-- 6. Board Column Additional Statuses join table
CREATE TABLE `board_column_additional_statuses` (
  `board_column_id` char(36) NOT NULL,
  `status_id` char(36) NOT NULL,
  PRIMARY KEY (`board_column_id`, `status_id`),
  KEY `fk_bcas_status` (`status_id`),
  CONSTRAINT `fk_bcas_board_column` FOREIGN KEY (`board_column_id`) REFERENCES `board_columns` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bcas_status` FOREIGN KEY (`status_id`) REFERENCES `statuses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
