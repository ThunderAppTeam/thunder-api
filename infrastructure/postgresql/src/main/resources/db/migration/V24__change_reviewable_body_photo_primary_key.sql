DO $$
    DECLARE
        primary_key_name TEXT;
    BEGIN
        SELECT conname
        INTO primary_key_name
        FROM pg_constraint
        WHERE conrelid = 'reviewable_body_photo'::regclass
          AND contype = 'p';

        IF primary_key_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE reviewable_body_photo DROP CONSTRAINT %I', primary_key_name);
        END IF;
    END $$;

DELETE FROM reviewable_body_photo r
    USING reviewable_body_photo k
WHERE r.member_id = k.member_id
  AND r.body_photo_id = k.body_photo_id
  AND (k.created_at, k.ctid) < (r.created_at, r.ctid);

ALTER TABLE reviewable_body_photo
    ADD PRIMARY KEY (member_id, body_photo_id);

DROP INDEX IF EXISTS idx_reviewable_body_photo_member_id_body_photo_id;
