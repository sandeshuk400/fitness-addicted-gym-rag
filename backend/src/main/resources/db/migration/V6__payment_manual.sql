-- V6: manual (front-desk) payments on top of the V1 payment table
CREATE SEQUENCE payment_receipt_seq START 1;

ALTER TABLE payment
    ADD COLUMN method         VARCHAR(20) CHECK (method IN ('CASH', 'UPI', 'CARD')),
    ADD COLUMN reference      VARCHAR(100),
    ADD COLUMN receipt_number VARCHAR(30) UNIQUE,
    ADD COLUMN recorded_by    BIGINT REFERENCES app_user (id),
    ADD COLUMN refunded_at    TIMESTAMPTZ,
    ADD COLUMN refunded_by    BIGINT REFERENCES app_user (id),
    ADD COLUMN refund_reason  VARCHAR(500);