-- Run this ONCE in MySQL before restarting the app, if V5 failed halfway.
-- It restores the DB to the pre-V5 state so Flyway can re-run the fixed V5.

SET FOREIGN_KEY_CHECKS = 0;

-- Drop empty UUID tables created before the failure (INSERTs never ran)
DROP TABLE IF EXISTS verification_tokens;
DROP TABLE IF EXISTS user_identities;
DROP TABLE IF EXISTS pending_signups;
DROP TABLE IF EXISTS todo_tags;
DROP TABLE IF EXISTS todos;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS board_members;
DROP TABLE IF EXISTS boards;
DROP TABLE IF EXISTS users;

-- Drop mapping tables from the failed attempt
DROP TABLE IF EXISTS tmp_map_users;
DROP TABLE IF EXISTS tmp_map_boards;
DROP TABLE IF EXISTS tmp_map_todos;
DROP TABLE IF EXISTS tmp_map_tags;
DROP TABLE IF EXISTS tmp_map_pending_signups;
DROP TABLE IF EXISTS tmp_map_verification_tokens;
DROP TABLE IF EXISTS tmp_map_user_identities;

-- Restore original table names (only if *_old exists from the failed rename)
RENAME TABLE
    users_old TO users,
    boards_old TO boards,
    board_members_old TO board_members,
    todos_old TO todos,
    tags_old TO tags,
    todo_tags_old TO todo_tags,
    pending_signups_old TO pending_signups,
    verification_tokens_old TO verification_tokens,
    user_identities_old TO user_identities;

SET FOREIGN_KEY_CHECKS = 1;

-- Clear the failed Flyway V5 row so it can run again
DELETE FROM flyway_schema_history WHERE version = '5';
