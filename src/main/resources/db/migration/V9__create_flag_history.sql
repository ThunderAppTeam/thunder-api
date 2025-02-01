CREATE TABLE IF NOT EXISTS flag_history
(
    flag_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    member_id       BIGINT    NOT NULL,
    body_photo_id   BIGINT    NOT NULL,
    flag_reason     TEXT      NOT NULL,
    other_reason    TEXT,
    created_at      TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_flag_history_member_id_body_photo_id
    ON flag_history (member_id, body_photo_id);
