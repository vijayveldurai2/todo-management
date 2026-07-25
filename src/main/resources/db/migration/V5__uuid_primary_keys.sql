-- ============================================================
-- V5: Convert all primary/foreign keys from BIGINT to UUID (CHAR(36))
-- Example id: f47ac10b-58cc-4372-a567-0e02b2c3d479
--
-- Note: MySQL FK constraint names are unique per schema. After RENAME,
-- old tables still hold names like fk_verification_pending, so new
-- tables must use different constraint names (fk_v5_...).
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- Mapping tables: old BIGINT id -> new UUID
-- ------------------------------------------------------------
CREATE TABLE tmp_map_users (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_boards (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_todos (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_tags (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_pending_signups (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_verification_tokens (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);
CREATE TABLE tmp_map_user_identities (
    old_id BIGINT PRIMARY KEY,
    new_id CHAR(36) NOT NULL
);

INSERT INTO tmp_map_users (old_id, new_id)
SELECT id, UUID() FROM users;

INSERT INTO tmp_map_boards (old_id, new_id)
SELECT id, UUID() FROM boards;

INSERT INTO tmp_map_todos (old_id, new_id)
SELECT id, UUID() FROM todos;

INSERT INTO tmp_map_tags (old_id, new_id)
SELECT id, UUID() FROM tags;

INSERT INTO tmp_map_pending_signups (old_id, new_id)
SELECT id, UUID() FROM pending_signups;

INSERT INTO tmp_map_verification_tokens (old_id, new_id)
SELECT id, UUID() FROM verification_tokens;

INSERT INTO tmp_map_user_identities (old_id, new_id)
SELECT id, UUID() FROM user_identities;

-- ------------------------------------------------------------
-- Rename old tables
-- ------------------------------------------------------------
RENAME TABLE
    users TO users_old,
    boards TO boards_old,
    board_members TO board_members_old,
    todos TO todos_old,
    tags TO tags_old,
    todo_tags TO todo_tags_old,
    pending_signups TO pending_signups_old,
    verification_tokens TO verification_tokens_old,
    user_identities TO user_identities_old;

-- ------------------------------------------------------------
-- Recreate tables with UUID keys (unique FK names: fk_v5_*)
-- ------------------------------------------------------------
CREATE TABLE users (
    id               CHAR(36)     NOT NULL PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL,
    email            VARCHAR(100) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    location         VARCHAR(255) NULL,
    avatar_url       VARCHAR(500) NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
    plan             VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    email_verified_at DATETIME    NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at    DATETIME     NULL,
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE boards (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    owner_id    CHAR(36)     NOT NULL,
    CONSTRAINT fk_v5_boards_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE board_members (
    board_id CHAR(36) NOT NULL,
    user_id  CHAR(36) NOT NULL,
    PRIMARY KEY (board_id, user_id),
    CONSTRAINT fk_v5_board_members_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_v5_board_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE tags (
    id   CHAR(36)    NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_tags_name (name)
);

CREATE TABLE todos (
    id             CHAR(36)     NOT NULL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    description    VARCHAR(255) NOT NULL,
    priority       VARCHAR(20)  NOT NULL,
    completed      BOOLEAN      NOT NULL,
    created_date   DATETIME     NOT NULL,
    modified_date  DATETIME     NOT NULL,
    due_date       DATETIME     NULL,
    completed_date DATETIME     NULL,
    board_id       CHAR(36)     NULL,
    CONSTRAINT fk_v5_todos_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE SET NULL
);

CREATE TABLE todo_tags (
    todo_id CHAR(36) NOT NULL,
    tag_id  CHAR(36) NOT NULL,
    PRIMARY KEY (todo_id, tag_id),
    CONSTRAINT fk_v5_todo_tags_todo FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE CASCADE,
    CONSTRAINT fk_v5_todo_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE pending_signups (
    id                     CHAR(36)     NOT NULL PRIMARY KEY,
    email                  VARCHAR(100) NOT NULL,
    username               VARCHAR(50)  NOT NULL,
    password               VARCHAR(255) NULL,
    name                   VARCHAR(100) NULL,
    primary_login_method   VARCHAR(20)  NOT NULL DEFAULT 'PASSWORD',
    oauth_provider_subject VARCHAR(255) NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at             DATETIME     NOT NULL,
    UNIQUE KEY uk_pending_signups_email (email),
    UNIQUE KEY uk_pending_signups_username (username)
);

CREATE TABLE verification_tokens (
    id                 CHAR(36)     NOT NULL PRIMARY KEY,
    pending_signup_id  CHAR(36)     NULL,
    user_id            CHAR(36)     NULL,
    token_hash         VARCHAR(255) NOT NULL,
    purpose            VARCHAR(30)  NOT NULL,
    expires_at         DATETIME     NOT NULL,
    used_at            DATETIME     NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_verification_tokens_token_hash (token_hash),
    CONSTRAINT fk_v5_verification_pending
        FOREIGN KEY (pending_signup_id) REFERENCES pending_signups(id) ON DELETE CASCADE,
    CONSTRAINT fk_v5_verification_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_identities (
    id               CHAR(36)     NOT NULL PRIMARY KEY,
    user_id          CHAR(36)     NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_identities_provider_subject (provider, provider_subject),
    CONSTRAINT fk_v5_user_identities_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- Copy data with new UUIDs
-- ------------------------------------------------------------
INSERT INTO users (
    id, username, email, password, name, location, avatar_url,
    is_active, role, plan, email_verified_at, created_at, updated_at, last_login_at
)
SELECT
    m.new_id,
    u.username,
    u.email,
    u.password,
    u.name,
    u.location,
    u.avatar_url,
    u.is_active,
    u.role,
    u.plan,
    u.email_verified_at,
    u.created_at,
    u.updated_at,
    u.last_login_at
FROM users_old u
JOIN tmp_map_users m ON m.old_id = u.id;

INSERT INTO boards (id, name, description, owner_id)
SELECT
    mb.new_id,
    b.name,
    b.description,
    mu.new_id
FROM boards_old b
JOIN tmp_map_boards mb ON mb.old_id = b.id
JOIN tmp_map_users mu ON mu.old_id = b.owner_id;

INSERT INTO board_members (board_id, user_id)
SELECT mb.new_id, mu.new_id
FROM board_members_old bm
JOIN tmp_map_boards mb ON mb.old_id = bm.board_id
JOIN tmp_map_users mu ON mu.old_id = bm.user_id;

INSERT INTO tags (id, name)
SELECT m.new_id, t.name
FROM tags_old t
JOIN tmp_map_tags m ON m.old_id = t.id;

INSERT INTO todos (
    id, title, description, priority, completed,
    created_date, modified_date, due_date, completed_date, board_id
)
SELECT
    mt.new_id,
    t.title,
    t.description,
    t.priority,
    t.completed,
    t.created_date,
    t.modified_date,
    t.due_date,
    t.completed_date,
    mb.new_id
FROM todos_old t
JOIN tmp_map_todos mt ON mt.old_id = t.id
LEFT JOIN tmp_map_boards mb ON mb.old_id = t.board_id;

INSERT INTO todo_tags (todo_id, tag_id)
SELECT mt.new_id, mg.new_id
FROM todo_tags_old tt
JOIN tmp_map_todos mt ON mt.old_id = tt.todo_id
JOIN tmp_map_tags mg ON mg.old_id = tt.tag_id;

INSERT INTO pending_signups (
    id, email, username, password, name, primary_login_method,
    oauth_provider_subject, created_at, expires_at
)
SELECT
    m.new_id,
    p.email,
    p.username,
    p.password,
    p.name,
    p.primary_login_method,
    p.oauth_provider_subject,
    p.created_at,
    p.expires_at
FROM pending_signups_old p
JOIN tmp_map_pending_signups m ON m.old_id = p.id;

INSERT INTO verification_tokens (
    id, pending_signup_id, user_id, token_hash, purpose, expires_at, used_at, created_at
)
SELECT
    mv.new_id,
    mp.new_id,
    mu.new_id,
    v.token_hash,
    v.purpose,
    v.expires_at,
    v.used_at,
    v.created_at
FROM verification_tokens_old v
JOIN tmp_map_verification_tokens mv ON mv.old_id = v.id
LEFT JOIN tmp_map_pending_signups mp ON mp.old_id = v.pending_signup_id
LEFT JOIN tmp_map_users mu ON mu.old_id = v.user_id;

INSERT INTO user_identities (id, user_id, provider, provider_subject, created_at)
SELECT
    mi.new_id,
    mu.new_id,
    i.provider,
    i.provider_subject,
    i.created_at
FROM user_identities_old i
JOIN tmp_map_user_identities mi ON mi.old_id = i.id
JOIN tmp_map_users mu ON mu.old_id = i.user_id;

-- ------------------------------------------------------------
-- Drop old tables + maps
-- ------------------------------------------------------------
DROP TABLE board_members_old;
DROP TABLE todo_tags_old;
DROP TABLE verification_tokens_old;
DROP TABLE user_identities_old;
DROP TABLE todos_old;
DROP TABLE tags_old;
DROP TABLE boards_old;
DROP TABLE pending_signups_old;
DROP TABLE users_old;

DROP TABLE tmp_map_users;
DROP TABLE tmp_map_boards;
DROP TABLE tmp_map_todos;
DROP TABLE tmp_map_tags;
DROP TABLE tmp_map_pending_signups;
DROP TABLE tmp_map_verification_tokens;
DROP TABLE tmp_map_user_identities;

SET FOREIGN_KEY_CHECKS = 1;
