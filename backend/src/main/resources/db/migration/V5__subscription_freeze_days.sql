-- V5: track how many freeze days a subscription has used (enforces the per-subscription freeze limit)

ALTER TABLE subscription
    ADD COLUMN frozen_days_used INT NOT NULL DEFAULT 0 CHECK (frozen_days_used >= 0);
