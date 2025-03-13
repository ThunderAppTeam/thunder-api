ALTER TABLE IF EXISTS member_fcm_token
    ADD COLUMN IF NOT EXISTS updated_at timestamp;
