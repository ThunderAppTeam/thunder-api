DELETE FROM dummy_deck r
    USING dummy_deck k
WHERE r.member_id = k.member_id
  AND r.body_photo_id = k.body_photo_id
  AND k.dummy_deck_id < r.dummy_deck_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_dummy_deck_member_id_body_photo_id
    ON dummy_deck (member_id, body_photo_id);
