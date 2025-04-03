ALTER TABLE body_photo DROP COLUMN IF EXISTS is_review_completed;

ALTER TABLE body_photo ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;
