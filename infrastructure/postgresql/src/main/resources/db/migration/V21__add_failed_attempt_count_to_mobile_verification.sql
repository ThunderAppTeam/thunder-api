ALTER TABLE mobile_verification ADD COLUMN IF NOT EXISTS failed_attempt_count INT NOT NULL DEFAULT 0;
