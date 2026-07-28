-- Users table
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(100) NOT NULL
);

-- Boards table
CREATE TABLE boards (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        description VARCHAR(500),
                        owner_id BIGINT NOT NULL,
                        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Board members (many-to-many: users added to a board by the owner)
CREATE TABLE board_members (
                               board_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               PRIMARY KEY (board_id, user_id),
                               FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Since todos already has existing rows, we add nullable first,
-- backfill, then enforce NOT NULL — same pattern as before.
ALTER TABLE todos ADD COLUMN board_id BIGINT;