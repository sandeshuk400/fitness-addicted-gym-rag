-- Device user id: the number the fingerprint terminal knows the member by (no biometric data is stored here)
ALTER TABLE member_profile ADD COLUMN device_user_id INTEGER;
CREATE UNIQUE INDEX uq_member_profile_device_user ON member_profile (device_user_id);

-- One attendance row per member per day (Indian time)
ALTER TABLE attendance ADD COLUMN visit_date DATE;
UPDATE attendance SET visit_date = (check_in AT TIME ZONE 'Asia/Kolkata')::date;
ALTER TABLE attendance ALTER COLUMN visit_date SET NOT NULL;
CREATE UNIQUE INDEX uq_attendance_member_day ON attendance (member_id, visit_date);

-- Drop any old CHECK on the source column so FINGERPRINT is allowed
DO $$
DECLARE c record;
BEGIN
  FOR c IN SELECT conname FROM pg_constraint
           WHERE conrelid = 'attendance'::regclass AND contype = 'c'
             AND pg_get_constraintdef(oid) ILIKE '%source%'
  LOOP
    EXECUTE format('ALTER TABLE attendance DROP CONSTRAINT %I', c.conname);
  END LOOP;
END $$;

-- Every scan, unlimited
CREATE TABLE door_event (
  id             BIGSERIAL PRIMARY KEY,
  device_serial  VARCHAR(50)  NOT NULL,
  device_user_id INTEGER      NOT NULL,
  member_id      BIGINT,
  scanned_at     TIMESTAMPTZ  NOT NULL,
  received_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  result         VARCHAR(30)  NOT NULL
);
CREATE UNIQUE INDEX uq_door_event_scan ON door_event (device_serial, device_user_id, scanned_at);
CREATE INDEX ix_door_event_member ON door_event (member_id, scanned_at DESC);