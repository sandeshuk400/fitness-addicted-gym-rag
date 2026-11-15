-- Allow the fingerprint door consent purpose in consent_record
DO $$
DECLARE c record;
BEGIN
  FOR c IN SELECT conname FROM pg_constraint
           WHERE conrelid = 'consent_record'::regclass AND contype = 'c'
             AND pg_get_constraintdef(oid) ILIKE '%purpose%'
  LOOP
    EXECUTE format('ALTER TABLE consent_record DROP CONSTRAINT %I', c.conname);
  END LOOP;
END $$;

ALTER TABLE consent_record ADD CONSTRAINT consent_record_purpose_check
  CHECK (purpose IN ('TERMS', 'HEALTH_DATA', 'MARKETING', 'AI_PROCESSING', 'BIOMETRIC_DOOR'));