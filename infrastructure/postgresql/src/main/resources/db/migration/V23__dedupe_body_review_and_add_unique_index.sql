DELETE FROM body_review r
    USING body_review k
WHERE r.body_photo_id = k.body_photo_id
  AND r.member_id = k.member_id
  AND (k.created_at, k.body_review_id) < (r.created_at, r.body_review_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_body_review_body_photo_id_member_id
    ON body_review (body_photo_id, member_id);
