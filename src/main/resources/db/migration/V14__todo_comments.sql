-- Parent references deliberately do not cascade recursively (unlimited depth).
-- TodoService detaches parent links under the Todo lock before deleting the Todo.
CREATE TABLE comments (
  id char(36) NOT NULL,
  todo_id char(36) NOT NULL,
  author_id char(36) NOT NULL,
  parent_comment_id char(36) NULL,
  content_json JSON NOT NULL,
  content_plain_text LONGTEXT NOT NULL,
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at datetime(6) NULL,
  deleted_by char(36) NULL,
  version int NOT NULL DEFAULT 0,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_comments_todo_created_id (todo_id, created_at, id),
  CONSTRAINT fk_comments_todo FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id),
  CONSTRAINT fk_comments_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

