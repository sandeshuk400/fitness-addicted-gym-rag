-- A payment can be refunded only once, so it can produce at most one refund notice.
CREATE UNIQUE INDEX uq_notification_payment_refund
    ON notification (payment_id)
    WHERE type = 'PAYMENT_REFUND';