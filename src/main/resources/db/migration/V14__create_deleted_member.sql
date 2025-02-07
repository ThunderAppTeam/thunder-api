CREATE TABLE IF NOT EXISTS deleted_member
(
    member_id       BIGINT PRIMARY KEY,
    member_uuid     UUID      NOT NULL,
    nickname        TEXT      NOT NULL,
    mobile_number   TEXT      NOT NULL,
    deletion_reason TEXT      NOT NULL,
    other_reason    TEXT,
    deleted_at      TIMESTAMP NOT NULL
);
