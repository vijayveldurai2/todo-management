-- ============================================================
-- V4: Auth foundation tables
--
-- What this does (in order):
--   1. Evolve existing `users` with auth/profile columns
--   2. Create `pending_signups`  (unverified signups live here)
--   3. Create `verification_tokens` (email verify / password reset)
--   4. Create `user_identities` (PASSWORD, GOOGLE, etc.)
--
-- Note: we keep the column name `password` for now so the current
-- User Java entity still matches the DB (ddl-auto=validate).
-- Later we will rename it to `password_hash` when we update the entity.
-- ============================================================


-- ------------------------------------------------------------
-- 1) Evolve `users`
--    Only verified accounts belong here.
--    email_verified_at = when they finished verification.
--    is_active         = admin can disable/ban without deleting.
-- ------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN location          VARCHAR(255) NULL,
    ADD COLUMN avatar_url        VARCHAR(500) NULL,
    ADD COLUMN is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN role              VARCHAR(20)  NOT NULL DEFAULT 'USER',
    ADD COLUMN plan              VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    ADD COLUMN email_verified_at DATETIME     NULL,
    ADD COLUMN created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN last_login_at     DATETIME     NULL;

-- Existing rows were created before this auth flow — treat them as already verified.
UPDATE users
SET email_verified_at = CURRENT_TIMESTAMP
WHERE email_verified_at IS NULL;


-- ------------------------------------------------------------
-- 2) `pending_signups`
--    People who signed up but have NOT verified email yet.
--    After verify, we copy them into `users` and delete this row.
-- ------------------------------------------------------------
CREATE TABLE pending_signups (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                 VARCHAR(100) NOT NULL,
    username              VARCHAR(50)  NOT NULL,
    -- Will hold a BCrypt hash later; NULL if signup is OAuth-only (e.g. Google)
    password              VARCHAR(255) NULL,
    name                  VARCHAR(100) NULL,
    -- Hint for how they started signup: PASSWORD | GOOGLE | ...
    primary_login_method  VARCHAR(20)  NOT NULL DEFAULT 'PASSWORD',
    -- Google "sub" (or similar) when signing up via OAuth; otherwise NULL
    oauth_provider_subject VARCHAR(255) NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Pending rows should expire if never verified (app/job can clean them up)
    expires_at            DATETIME     NOT NULL,

    UNIQUE KEY uk_pending_signups_email (email),
    UNIQUE KEY uk_pending_signups_username (username)
);


-- ------------------------------------------------------------
-- 3) `verification_tokens`
--    Opaque tokens sent by email. We store a HASH of the token,
--    never the raw token itself.
--
--    purpose examples:
--      EMAIL_VERIFY   -> linked to pending_signups
--      PASSWORD_RESET -> linked to users
-- ------------------------------------------------------------
CREATE TABLE verification_tokens (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    pending_signup_id  BIGINT NULL,
    user_id            BIGINT NULL,
    token_hash         VARCHAR(255) NOT NULL,
    purpose            VARCHAR(30)  NOT NULL,
    expires_at         DATETIME     NOT NULL,
    used_at            DATETIME     NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_verification_tokens_token_hash (token_hash),
    CONSTRAINT fk_verification_pending
        FOREIGN KEY (pending_signup_id) REFERENCES pending_signups(id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- ------------------------------------------------------------
-- 4) `user_identities`
--    How a verified user can log in.
--    One user may have multiple rows later (password + Google).
-- ------------------------------------------------------------
CREATE TABLE user_identities (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    -- PASSWORD | GOOGLE | ...
    provider         VARCHAR(20)  NOT NULL,
    -- Stable id from the provider (e.g. email for PASSWORD, Google "sub" for GOOGLE)
    provider_subject VARCHAR(255) NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_identities_provider_subject (provider, provider_subject),
    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
