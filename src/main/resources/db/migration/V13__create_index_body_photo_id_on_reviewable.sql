DROP TABLE IF EXISTS review_rotation_queue;

DROP INDEX IF EXISTS idx_reviewable_body_photo_body_photo_member_id;

CREATE INDEX IF NOT EXISTS idx_reviewable_body_photo_body_photo_id
    ON reviewable_body_photo (body_photo_id);
