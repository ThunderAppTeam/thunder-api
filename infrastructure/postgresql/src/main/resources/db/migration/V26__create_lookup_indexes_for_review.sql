CREATE INDEX IF NOT EXISTS idx_body_review_member_id
    ON body_review (member_id);

CREATE INDEX IF NOT EXISTS idx_reviewable_body_photo_body_photo_member_id
    ON reviewable_body_photo (body_photo_member_id);
