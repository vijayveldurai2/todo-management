-- V10: Todo dates, estimates, story points, todo_roles, todo_assignments

-- ============================================================
-- 1. Add scheduling + estimate + story-point columns to todos
-- ============================================================
ALTER TABLE `todos`
  ADD COLUMN `start_date_time` datetime     DEFAULT NULL          AFTER `due_date`,
  ADD COLUMN `end_date_time`   datetime     DEFAULT NULL          AFTER `start_date_time`,
  ADD COLUMN `estimated_time`  decimal(8,2) DEFAULT NULL          AFTER `end_date_time`,
  ADD COLUMN `remaining_time`  decimal(8,2) DEFAULT NULL          AFTER `estimated_time`,
  ADD COLUMN `story_points`    smallint     DEFAULT NULL          AFTER `remaining_time`;

-- Note: due_date column is retained (nullable). Application layer stops writing
-- to it; a follow-up cleanup migration can DROP it once fully migrated.

-- ============================================================
-- 2. todo_roles — project-scoped, admin-customisable role labels
--    (no authorization semantics — purely descriptive/functional)
-- ============================================================
CREATE TABLE `todo_roles` (
  `id`         char(36)     NOT NULL,
  `project_id` char(36)     NOT NULL,
  `name`       varchar(100) NOT NULL,  -- e.g. "Developer", "QA", "Supervisor"
  `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_roles_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_todo_roles_project`
    FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. todo_assignments — many-to-many (todo, user, role) with primary flag
-- ============================================================
CREATE TABLE `todo_assignments` (
  `id`           char(36)   NOT NULL,
  `todo_id`      char(36)   NOT NULL,
  `user_id`      char(36)   NOT NULL,
  `todo_role_id` char(36)   NOT NULL,
  `is_primary`   tinyint(1) NOT NULL DEFAULT 0,  -- at most one primary per todo (enforced at service layer)
  `created_at`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_assignments_todo_user_role` (`todo_id`, `user_id`, `todo_role_id`),
  KEY `fk_ta_todo`  (`todo_id`),
  KEY `fk_ta_user`  (`user_id`),
  KEY `fk_ta_role`  (`todo_role_id`),
  CONSTRAINT `fk_ta_todo`
    FOREIGN KEY (`todo_id`)      REFERENCES `todos`      (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ta_user`
    FOREIGN KEY (`user_id`)      REFERENCES `users`      (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ta_role`
    FOREIGN KEY (`todo_role_id`) REFERENCES `todo_roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
