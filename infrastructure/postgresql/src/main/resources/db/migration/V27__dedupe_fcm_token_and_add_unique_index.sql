DELETE FROM member_fcm_token t
    USING member_fcm_token k
WHERE t.fcm_token = k.fcm_token
  AND t.member_id <> k.member_id
  AND (COALESCE(k.updated_at, k.created_at), k.created_at, k.member_id)
    > (COALESCE(t.updated_at, t.created_at), t.created_at, t.member_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_member_fcm_token_fcm_token
    ON member_fcm_token (fcm_token);
