-- V4: Project Roles and Members Restructure
-- Adds custom roles per project and replaces the string-based role in project_members.

-- 1. Add status to projects
ALTER TABLE `projects`
  ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' AFTER `description`;

-- 2. Create project_roles
CREATE TABLE `project_roles` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `name` varchar(50) NOT NULL,
  `is_admin` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_roles_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_project_roles_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Restructure project_members
-- Drop the existing project_members table since this is development and data is disposable.
DROP TABLE IF EXISTS `project_members`;

CREATE TABLE `project_members` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `project_role_id` char(36) NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_members_project_user` (`project_id`,`user_id`),
  KEY `fk_project_members_user` (`user_id`),
  KEY `fk_project_members_role` (`project_role_id`),
  CONSTRAINT `fk_project_members_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_members_role` FOREIGN KEY (`project_role_id`) REFERENCES `project_roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
