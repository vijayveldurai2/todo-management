-- V4: Project roles — admin-defined per-project roles replacing the hard-coded enum.
-- Adds project_roles table and migrates project_members to FK reference it.

-- ============================================================
-- 1. PROJECT_ROLES — dynamic, admin-created roles per project
-- ============================================================
CREATE TABLE `project_roles` (
  `id` char(36) NOT NULL,
  `project_id` char(36) NOT NULL,
  `name` varchar(50) NOT NULL,           -- e.g. "Developer", "Tester", "Lead" — admin-defined
  `is_admin` tinyint(1) NOT NULL DEFAULT '0', -- if true, bearer has project admin rights
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_roles_project_name` (`project_id`,`name`),
  CONSTRAINT `fk_project_roles_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. PROJECT_MEMBERS — drop static role column, add FK to project_roles
-- ============================================================

-- Drop old static role column and add project_role_id FK
ALTER TABLE `project_members`
  DROP COLUMN `role`,
  ADD COLUMN `project_role_id` char(36) NOT NULL AFTER `user_id`,
  ADD COLUMN `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `project_role_id`,
  ADD KEY `fk_project_members_role` (`project_role_id`),
  ADD CONSTRAINT `fk_project_members_role` FOREIGN KEY (`project_role_id`) REFERENCES `project_roles` (`id`) ON DELETE RESTRICT;
