ALTER TABLE notification
    ADD COLUMN payment_id BIGINT REFERENCES payment (id) ON DELETE CASCADE;

-- A payment happens once, so it can produce at most one receipt notice.
CREATE UNIQUE INDEX uq_notification_payment_receipt
    ON notification (payment_id)
    WHERE type = 'PAYMENT_RECEIPT';