DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'body_photo' AND column_name = 'review_score'
        ) THEN
            ALTER TABLE body_photo RENAME COLUMN review_score TO total_review_score;
        END IF;
    END $$;
