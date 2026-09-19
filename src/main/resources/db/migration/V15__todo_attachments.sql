ALTER TABLE workspaces ADD COLUMN attachment_max_bytes BIGINT NULL;
CREATE TABLE todo_attachments (
  id char(36) NOT NULL PRIMARY KEY,
  todo_id char(36) NULL,
  uploaded_by char(36) NOT NULL,
  storage_key varchar(36) NOT NULL UNIQUE,
  file_name varchar(255) NOT NULL,
  size_bytes bigint NOT NULL,
  deleted boolean NOT NULL DEFAULT false,
  created_at datetime(6) NOT NULL,
  KEY idx_attachments_todo_created (todo_id, deleted, created_at, id),
  KEY idx_attachments_cleanup (deleted, todo_id),
  CONSTRAINT fk_attachments_todo FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE SET NULL,
  CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

