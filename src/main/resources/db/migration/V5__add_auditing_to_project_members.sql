-- V5: Add auditing timestamps to project_members.
-- created_at / updated_at were missing from the V4 schema; added here.

ALTER TABLE `project_members`
  ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `joined_at`,
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;
