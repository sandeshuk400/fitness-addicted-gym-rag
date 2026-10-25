-- Email delivery: count send attempts so a failing message is retried a limited number of times.
ALTER TABLE notification
    ADD COLUMN attempts INT NOT NULL DEFAULT 0;

-- The receipt and refund indexes ignored the channel, so an EMAIL row for the same payment would
-- clash with the IN_APP row. Recreate them per channel.
DROP INDEX uq_notification_payment_receipt;
DROP INDEX uq_notification_payment_refund;

CREATE UNIQUE INDEX uq_notification_payment_receipt
    ON notification (payment_id, channel)
    WHERE type = 'PAYMENT_RECEIPT';

CREATE UNIQUE INDEX uq_notification_payment_refund
    ON notification (payment_id, channel)
    WHERE type = 'PAYMENT_REFUND';