-- =============================================================================
-- FINAL CONSOLIDATED SCHEMA (Cumulative V1 through V7)
-- =============================================================================
-- Database: MySQL 8.0+
-- Character Set: utf8mb4, Collation: utf8mb4_0900_ai_ci
-- Tables ordered to respect foreign key constraints.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USERS
-- -----------------------------------------------------------------------------
CREATE TABLE `users` (
  `id` char(36) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `role` varchar(20) NOT NULL DEFAULT 'USER',
  `plan` varchar(20) NOT NULL DEFAULT 'FREE',
  `email_verified_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 2. PENDING_SIGNUPS
-- -----------------------------------------------------------------------------
CREATE TABLE `pending_signups` (
  `id` char(36) NOT NULL,
  `email` varchar(100) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `primary_login_method` varchar(20) NOT NULL DEFAULT 'PASSWORD',
  `oauth_provider_subject` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pending_signups_email` (`email`),
  UNIQUE KEY `uk_pending_signups_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 3. USER_IDENTITIES
-- -----------------------------------------------------------------------------
CREATE TABLE `user_identities` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `provider` varchar(20) NOT NULL,
  `provider_subject` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_identities_provider_subject` (`provider`, `provider_subject`),
  KEY `fk_user_identities_user` (`user_id`),
  CONSTRAINT `fk_user_identities_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 4. VERIFICATION_TOKENS
-- -----------------------------------------------------------------------------
CREATE TABLE `verification_tokens` (
  `id` char(36) NOT NULL,
  `pending_signup_id` char(36) DEFAULT NULL,
  `user_id` char(36) DEFAULT NULL,
  `token_hash` varchar(255) NOT NULL,
  `purpose` varchar(30) NOT NULL,
  `expires_at` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_verification_tokens_token_hash` (`token_hash`),
  KEY `fk_verification_pending` (`pending_signup_id`),
  KEY `fk_verification_user` (`user_id`),
  CONSTRAINT `fk_verification_pending` FOREIGN KEY (`pending_signup_id`) REFERENCES `pending_signups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_verification_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 5. TAGS
-- -----------------------------------------------------------------------------
CREATE TABLE `tags` (
  `id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 6. WORKSPACES
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 7. WORKSPACE_MEMBERS
-- -----------------------------------------------------------------------------
CREATE TABLE `workspace_members` (
  `id` char(36) NOT NULL,
  `workspace_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER', -- SUPER_ADMIN, USER
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INVITED, REMOVED
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspace_members_ws_user` (`workspace_id`, `user_id`),
  KEY `fk_workspace_members_user` (`user_id`),
  CONSTRAINT `fk_workspace_members_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workspace_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 8. WORKSPACE_INVITES
-- -----------------------------------------------------------------------------
CREATE TABLE `workspace_invites` (
  `id` char(36) NOT NULL,
  `workspace_id` char(36) NOT NULL,
  `email` varchar(100) NOT NULL,
  `invited_by` char(36) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, EXPIRED, REVOKED
  `token_hash` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `accepted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspace_invites_token_hash` (`token_hash`),
  KEY `fk_workspace_invites_invited_by` (`invited_by`),
  KEY `idx_workspace_invites_ws_email_status` (`workspace_id`, `email`, `status`),
  CONSTRAINT `fk_workspace_invites_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workspace_invites_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 9. PROJECTS
-- -----------------------------------------------------------------------------
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
  UNIQUE KEY `uk_projects_ws_slug` (`workspace_id`, `slug`),
  UNIQUE KEY `uk_projects_ws_prefix` (`workspace_id`, `prefix_code`),
  KEY `fk_projects_created_by` (`created_by`),
  CONSTRAINT `fk_projects_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_projects_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 10. PROJECT_ROLES
-- -----------------------------------------------------------------------------
CREATE TABLE `project_roles` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `name` varchar(50) NOT NULL,           -- e.g. "Developer", "Tester", "Lead"
  `is_admin` tinyint(1) NOT NULL DEFAULT '0', -- if true, bearer has project admin rights
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_roles_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_project_roles_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 11. PROJECT_MEMBERS
-- -----------------------------------------------------------------------------
CREATE TABLE `project_members` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `project_role_id` char(36) NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_members_project_user` (`project_id`, `user_id`),
  KEY `fk_project_members_user` (`user_id`),
  KEY `fk_project_members_role` (`project_role_id`),
  CONSTRAINT `fk_project_members_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_members_role` FOREIGN KEY (`project_role_id`) REFERENCES `project_roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 12. STATUSES
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 13. BOARDS (Base table for Joined Inheritance)
-- -----------------------------------------------------------------------------
CREATE TABLE `boards` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `board_type` varchar(30) NOT NULL DEFAULT 'KANBAN',
  `slug` varchar(120) NOT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'BOARD', -- BOARD, SPRINT
  `position` int NOT NULL DEFAULT 0,           -- drag-and-drop order within project
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_boards_project_slug` (`project_id`, `slug`),
  KEY `fk_boards_project` (`project_id`),
  CONSTRAINT `fk_boards_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 14. KANBAN_BOARDS (Subclass table)
-- -----------------------------------------------------------------------------
CREATE TABLE `kanban_boards` (
  `id` char(36) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_kanban_boards_board` FOREIGN KEY (`id`) REFERENCES `boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 15. SPRINT_BOARDS (Subclass table)
-- -----------------------------------------------------------------------------
CREATE TABLE `sprint_boards` (
  `id` char(36) NOT NULL,
  `sprint_start_date` date DEFAULT NULL,
  `sprint_end_date` date DEFAULT NULL,
  `sprint_goal` varchar(1000) DEFAULT NULL,
  `sprint_status` varchar(30) NOT NULL DEFAULT 'PLANNING',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_sprint_boards_board` FOREIGN KEY (`id`) REFERENCES `boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 16. BOARD_COLUMNS
-- -----------------------------------------------------------------------------
CREATE TABLE `board_columns` (
  `id` char(36) NOT NULL,
  `board_id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `position` int NOT NULL,                     -- drag-and-drop order within board
  `primary_status_id` char(36) NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0', -- marks default columns (0/1)
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_board_columns_board` (`board_id`),
  KEY `fk_board_columns_primary_status` (`primary_status_id`),
  CONSTRAINT `fk_board_columns_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_board_columns_primary_status` FOREIGN KEY (`primary_status_id`) REFERENCES `statuses` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 17. BOARD_COLUMN_ADDITIONAL_STATUSES (ManyToMany join table)
-- -----------------------------------------------------------------------------
CREATE TABLE `board_column_additional_statuses` (
  `board_column_id` char(36) NOT NULL,
  `status_id` char(36) NOT NULL,
  PRIMARY KEY (`board_column_id`, `status_id`),
  KEY `fk_bcas_status` (`status_id`),
  CONSTRAINT `fk_bcas_board_column` FOREIGN KEY (`board_column_id`) REFERENCES `board_columns` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bcas_status` FOREIGN KEY (`status_id`) REFERENCES `statuses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 18. TODOS
-- -----------------------------------------------------------------------------
CREATE TABLE `todos` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `display_id` varchar(20) NOT NULL,           -- e.g. 'WR-1', 'WR-2'
  `title` varchar(255) NOT NULL,
  `description` varchar(255) NOT NULL,
  `priority` varchar(20) NOT NULL,
  `completed` tinyint(1) NOT NULL,
  `board_id` char(36) DEFAULT NULL,
  `column_id` char(36) NOT NULL,
  `position` int NOT NULL DEFAULT 0,           -- drag-and-drop order within column
  `created_date` datetime NOT NULL,
  `modified_date` datetime NOT NULL,
  `due_date` datetime DEFAULT NULL,
  `completed_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todos_project_display_id` (`project_id`, `display_id`),
  KEY `fk_todos_project` (`project_id`),
  KEY `fk_todos_board` (`board_id`),
  KEY `fk_todos_column` (`column_id`),
  CONSTRAINT `fk_todos_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_todos_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_todos_column` FOREIGN KEY (`column_id`) REFERENCES `board_columns` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 19. TODO_TAGS (ManyToMany join table)
-- -----------------------------------------------------------------------------
CREATE TABLE `todo_tags` (
  `todo_id` char(36) NOT NULL,
  `tag_id` char(36) NOT NULL,
  PRIMARY KEY (`todo_id`, `tag_id`),
  KEY `fk_todo_tags_tag` (`tag_id`),
  CONSTRAINT `fk_todo_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_todo_tags_todo` FOREIGN KEY (`todo_id`) REFERENCES `todos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
