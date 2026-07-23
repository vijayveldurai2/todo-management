-- Enforce NOT NULL where required, now that columns exist.
-- Existing rows get sensible defaults first, since you can't apply
-- NOT NULL directly if any existing row currently has NULL there.
UPDATE todos SET priority = 'MEDIUM' WHERE priority IS NULL;
UPDATE todos SET created_date = NOW() WHERE created_date IS NULL;
UPDATE todos SET modified_date = NOW() WHERE modified_date IS NULL;

ALTER TABLE todos
    MODIFY COLUMN priority VARCHAR(20) NOT NULL,
    MODIFY COLUMN created_date DATETIME NOT NULL,
    MODIFY COLUMN modified_date DATETIME NOT NULL;
-- due_date and completed_date stay nullable — no change needed, already correct.
