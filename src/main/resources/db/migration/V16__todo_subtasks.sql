-- A subtask is a full Todo. No recursive cascade: protect children on parent deletion.
ALTER TABLE todos
  ADD COLUMN parent_todo_id char(36) NULL,
  ADD KEY idx_todos_parent_created (parent_todo_id, created_date, id),
  ADD CONSTRAINT fk_todos_parent FOREIGN KEY (parent_todo_id) REFERENCES todos(id) ON DELETE RESTRICT;
