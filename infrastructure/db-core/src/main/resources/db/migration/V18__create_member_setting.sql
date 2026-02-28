CREATE TABLE IF NOT EXISTS member_setting
(
    member_id  BIGINT PRIMARY KEY,
    settings   JSONB     NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
);
