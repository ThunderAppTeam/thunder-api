CREATE TABLE IF NOT EXISTS reviewable_body_photo
(
    member_id            BIGINT    NOT NULL,
    body_photo_id        BIGINT    NOT NULL,
    body_photo_member_id BIGINT    NOT NULL,
    created_at           TIMESTAMP NOT NULL,
    PRIMARY KEY (member_id, created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_reviewable_body_photo_member_id_body_photo_id
    ON reviewable_body_photo (member_id, body_photo_id);

CREATE INDEX IF NOT EXISTS idx_reviewable_body_photo_body_photo_member_id
    ON reviewable_body_photo (body_photo_member_id);
