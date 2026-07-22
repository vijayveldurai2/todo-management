-- Add new columns to todos
ALTER TABLE todos
    ADD COLUMN priority VARCHAR(20),
    ADD COLUMN created_date DATETIME,
    ADD COLUMN modified_date DATETIME,
    ADD COLUMN due_date DATETIME,
    ADD COLUMN completed_date DATETIME;

-- New table: groups/labels
CREATE TABLE groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Join table: links todos <-> groups (many-to-many)
CREATE TABLE todo_groups (
     todo_id BIGINT NOT NULL,
     group_id BIGINT NOT NULL,
     PRIMARY KEY (todo_id, group_id),
     FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE CASCADE,
     FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);