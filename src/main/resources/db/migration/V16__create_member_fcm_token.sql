CREATE TABLE IF NOT EXISTS member_fcm_token
(
    member_id  BIGINT PRIMARY KEY,
    fcm_token  TEXT      NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);