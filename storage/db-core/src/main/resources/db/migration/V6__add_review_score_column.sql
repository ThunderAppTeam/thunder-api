ALTER TABLE body_photo
    ADD COLUMN IF NOT EXISTS review_score DOUBLE PRECISION NOT NULL DEFAULT 0;

DROP INDEX IF EXISTS idx_body_photo_member_id;

CREATE INDEX IF NOT EXISTS idx_body_photo_member_review_score ON body_photo (member_id, review_score DESC);
