ALTER TABLE notification
    ADD COLUMN title           VARCHAR(150),
    ADD COLUMN message         TEXT,
    ADD COLUMN subscription_id BIGINT REFERENCES subscription (id) ON DELETE CASCADE,
    ADD COLUMN days_before     INT,
    ADD COLUMN read_at         TIMESTAMPTZ;

-- One expiry reminder per subscription, per threshold, per channel: reruns can never duplicate.
CREATE UNIQUE INDEX uq_notification_expiry_reminder
    ON notification (subscription_id, days_before, channel)
    WHERE type = 'SUBSCRIPTION_EXPIRY';

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);