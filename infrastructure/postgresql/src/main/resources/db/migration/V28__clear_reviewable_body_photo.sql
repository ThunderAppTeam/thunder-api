-- reviewable_body_photo is derived data rebuilt by the queue refresher; rows written by older versions had member_id and body_photo_id swapped
DELETE FROM reviewable_body_photo;
