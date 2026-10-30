-- V11: tell plan payments apart from the one-time joining fee
ALTER TABLE payment
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'PLAN'
    CHECK (kind IN ('PLAN', 'JOINING_FEE'));