-- V5: Add missing auditing columns to project_members

ALTER TABLE `project_members` 
  ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
