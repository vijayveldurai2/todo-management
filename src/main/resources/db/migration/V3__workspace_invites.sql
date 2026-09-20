-- V3: Workspace invites — email-based invite/accept flow for workspace membership.
-- Replaces direct add-by-userId for workspaces. Project membership remains direct-add
-- (project members must already be workspace members, so no invite step needed there).

CREATE TABLE `workspace_invites` (
  `id` char(36) NOT NULL,
  `workspace_id` char(36) NOT NULL,
  `email` varchar(100) NOT NULL,
  `invited_by` char(36) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER', -- role the invitee will get on acceptance
  `status` varchar(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, EXPIRED, REVOKED
  `token_hash` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `accepted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspace_invites_token_hash` (`token_hash`),
  KEY `fk_workspace_invites_invited_by` (`invited_by`),
  KEY `idx_workspace_invites_ws_email_status` (`workspace_id`,`email`,`status`),
  CONSTRAINT `fk_workspace_invites_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workspace_invites_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
