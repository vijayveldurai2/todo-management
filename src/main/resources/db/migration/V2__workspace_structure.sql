-- V2: Workspace -> Project -> Board -> Column -> Todo restructure
-- Fresh-start migration (existing boards/todos/board_members data is disposable).

-- ============================================================
-- 1. WORKSPACES
-- ============================================================
CREATE TABLE `workspaces` (
  `id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `created_by` char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspaces_slug` (`slug`),
  KEY `fk_workspaces_created_by` (`created_by`),
  CONSTRAINT `fk_workspaces_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. WORKSPACE_MEMBERS
-- ============================================================
CREATE TABLE `workspace_members` (
  `id` char(36) NOT NULL,
  `workspace_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER', -- SUPER_ADMIN, USER
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INVITED, REMOVED
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspace_members_ws_user` (`workspace_id`,`user_id`),
  KEY `fk_workspace_members_user` (`user_id`),
  CONSTRAINT `fk_workspace_members_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workspace_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. PROJECTS
-- ============================================================
CREATE TABLE `projects` (
  `id` char(36) NOT NULL,
  `workspace_id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `prefix_code` varchar(10) NOT NULL,       -- e.g. 'WR' — immutable once set
  `display_id_seq` int NOT NULL DEFAULT 0,  -- atomically incremented for todo display_id generation
  `created_by` char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_projects_ws_slug` (`workspace_id`,`slug`),
  UNIQUE KEY `uk_projects_ws_prefix` (`workspace_id`,`prefix_code`),
  KEY `fk_projects_created_by` (`created_by`),
  CONSTRAINT `fk_projects_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_projects_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. PROJECT_MEMBERS
-- ============================================================
CREATE TABLE `project_members` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER', -- USER, DEVELOPER, TESTER, LEAD, ...
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_members_project_user` (`project_id`,`user_id`),
  KEY `fk_project_members_user` (`user_id`),
  CONSTRAINT `fk_project_members_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5. BOARDS — restructure to hang off projects, drop owner/board_members
-- ============================================================
DROP TABLE IF EXISTS `board_members`;

ALTER TABLE `boards`
  DROP FOREIGN KEY `fk_v5_boards_owner`,
  DROP COLUMN `owner_id`,
  ADD COLUMN `project_id` char(36) NOT NULL AFTER `id`,
  ADD COLUMN `slug` varchar(120) NOT NULL AFTER `name`,
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT 'BOARD' AFTER `slug`, -- BOARD, SPRINT
  ADD COLUMN `position` int NOT NULL DEFAULT 0 AFTER `type`, -- drag-and-drop order within project
  ADD UNIQUE KEY `uk_boards_project_slug` (`project_id`,`slug`),
  ADD CONSTRAINT `fk_boards_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE;

-- ============================================================
-- 6. BOARD_COLUMNS — configurable columns per board (Todo/Working/Testing/Done by default)
-- ============================================================
CREATE TABLE `board_columns` (
  `id` char(36) NOT NULL,
  `board_id` char(36) NOT NULL,
  `name` varchar(50) NOT NULL,
  `position` int NOT NULL,               -- drag-and-drop order within board
  `is_default` tinyint(1) NOT NULL DEFAULT '0', -- marks the 4 seeded columns; not user-deletable if true
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_board_columns_board` (`board_id`),
  CONSTRAINT `fk_board_columns_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Note: the 4 default columns (Todo, Working, Testing, Done) are NOT seeded here via SQL,
-- since board_id doesn't exist until a board is created. Seed them in BoardService.createBoard()
-- immediately after board insert, with is_default = true, positions 0-3.

-- ============================================================
-- 7. TODOS — add project_id, display_id (WR-546 style), column_id, position
-- ============================================================
ALTER TABLE `todos`
  ADD COLUMN `project_id` char(36) NOT NULL AFTER `id`,
  ADD COLUMN `display_id` varchar(20) NOT NULL AFTER `project_id`,
  ADD COLUMN `column_id` char(36) NOT NULL AFTER `board_id`,
  ADD COLUMN `position` int NOT NULL DEFAULT 0 AFTER `column_id`, -- drag-and-drop order within column
  ADD UNIQUE KEY `uk_todos_project_display_id` (`project_id`,`display_id`),
  ADD CONSTRAINT `fk_todos_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_todos_column` FOREIGN KEY (`column_id`) REFERENCES `board_columns` (`id`) ON DELETE RESTRICT;
