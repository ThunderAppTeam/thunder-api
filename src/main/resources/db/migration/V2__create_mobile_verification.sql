CREATE TABLE IF NOT EXISTS mobile_verification
(
    mobile_verification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mobile_number          TEXT      NOT NULL,
    mobile_country         TEXT      NOT NULL,
    verification_code      TEXT      NOT NULL,
    verified_at            TIMESTAMP,
    created_at             TIMESTAMP NOT NULL,
    expired_at             TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mobile_verification_mobile_number_expired_at ON mobile_verification (mobile_number, expired_at DESC);