CREATE TABLE IF NOT EXISTS mobile_verification
(
    mobile_verification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id              TEXT      NOT NULL,
    mobile_number          TEXT      NOT NULL,
    mobile_country         TEXT      NOT NULL,
    verification_code      TEXT      NOT NULL,
    verified_at            TIMESTAMP,
    created_at             TIMESTAMP NOT NULL,
    expired_at             TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mobile_verification_device_id_mobile_number ON mobile_verification (device_id, mobile_number);