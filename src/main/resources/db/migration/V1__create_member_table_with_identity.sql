CREATE TABLE member
(
    member_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nickname            TEXT UNIQUE NOT NULL,
    mobile_number       TEXT UNIQUE NOT NULL,
    mobile_country      TEXT        NOT NULL,
    gender              TEXT        NOT NULL,
    birth_day           DATE        NOT NULL,
    country_code        TEXT        NOT NULL,
    marketing_agreement BOOLEAN     NOT NULL,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP,
    updated_by          BIGINT
);