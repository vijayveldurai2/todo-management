-- V6: Add updated_at audit column to project_roles.
-- project_roles only had created_at; this adds updated_at with auto ON UPDATE.

ALTER TABLE `project_roles`
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;
