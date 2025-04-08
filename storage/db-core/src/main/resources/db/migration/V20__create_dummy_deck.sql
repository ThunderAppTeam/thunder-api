CREATE TABLE IF NOT EXISTS dummy_deck
(
    dummy_deck_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id            BIGINT    NOT NULL,
    body_photo_id        BIGINT    NOT NULL,
    body_photo_member_id BIGINT    NOT NULL,
    nickname             TEXT      NOT NULL,
    age                  INT       NOT NULL,
    created_at           TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dummy_deck_member_id_created_at
    ON dummy_deck (member_id, created_at);

CREATE INDEX IF NOT EXISTS idx_dummy_deck_body_photo_id
    ON dummy_deck (body_photo_id);
