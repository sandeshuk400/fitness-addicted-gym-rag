-- V1: core gym domain (users, memberships, attendance, classes, payments, audit)

CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('MEMBER', 'TRAINER', 'STAFF', 'ADMIN')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE member_profile (
    user_id           BIGINT PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    full_name         VARCHAR(150) NOT NULL,
    phone             VARCHAR(20),
    date_of_birth     DATE,
    gender            VARCHAR(20),
    emergency_contact VARCHAR(150),
    health_notes      TEXT,            -- sensitive: restricted access, never sent to the LLM
    joined_on         DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE trainer_profile (
    user_id        BIGINT PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    full_name      VARCHAR(150) NOT NULL,
    bio            TEXT,
    specialization VARCHAR(150),
    certifications VARCHAR(255)
);

CREATE TABLE membership_plan (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL UNIQUE,
    description   TEXT,
    duration_days INT            NOT NULL CHECK (duration_days > 0),
    price         NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    active        BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE subscription (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    BIGINT      NOT NULL REFERENCES member_profile (user_id),
    plan_id      BIGINT      NOT NULL REFERENCES membership_plan (id),
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'EXPIRED', 'CANCELLED')),
    frozen_until DATE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date >= start_date)
);
CREATE INDEX idx_subscription_member ON subscription (member_id);
CREATE INDEX idx_subscription_end_date_status ON subscription (end_date, status);

CREATE TABLE payment (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT         NOT NULL REFERENCES subscription (id),
    amount          NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    currency        VARCHAR(3)     NOT NULL DEFAULT 'INR',
    status          VARCHAR(20)    NOT NULL CHECK (status IN ('CREATED', 'PAID', 'FAILED', 'REFUNDED')),
    gateway         VARCHAR(30),
    gateway_ref     VARCHAR(100) UNIQUE,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_subscription ON payment (subscription_id);

CREATE TABLE attendance (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT      NOT NULL REFERENCES member_profile (user_id),
    check_in  TIMESTAMPTZ NOT NULL DEFAULT now(),
    check_out TIMESTAMPTZ,
    source    VARCHAR(20) NOT NULL CHECK (source IN ('QR', 'FRONT_DESK')),
    CHECK (check_out IS NULL OR check_out >= check_in)
);
CREATE INDEX idx_attendance_member_time ON attendance (member_id, check_in DESC);
CREATE INDEX idx_attendance_check_in ON attendance (check_in);

CREATE TABLE trainer_assignment (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trainer_id  BIGINT  NOT NULL REFERENCES trainer_profile (user_id),
    member_id   BIGINT  NOT NULL REFERENCES member_profile (user_id),
    assigned_on DATE    NOT NULL DEFAULT CURRENT_DATE,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_trainer_assignment_active
    ON trainer_assignment (trainer_id, member_id) WHERE active;

CREATE TABLE gym_class (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    trainer_id  BIGINT REFERENCES trainer_profile (user_id),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE class_session (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_id  BIGINT      NOT NULL REFERENCES gym_class (id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at   TIMESTAMPTZ NOT NULL,
    capacity  INT         NOT NULL CHECK (capacity > 0),
    room      VARCHAR(50),
    status    VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CANCELLED', 'DONE')),
    CHECK (ends_at > starts_at)
);
CREATE INDEX idx_class_session_starts_at ON class_session (starts_at);

CREATE TABLE booking (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id BIGINT      NOT NULL REFERENCES class_session (id),
    member_id  BIGINT      NOT NULL REFERENCES member_profile (user_id),
    status     VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'WAITLISTED', 'CANCELLED', 'ATTENDED', 'NO_SHOW')),
    booked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, member_id)
);
CREATE INDEX idx_booking_member ON booking (member_id);

CREATE TABLE training_plan (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id   BIGINT      NOT NULL REFERENCES member_profile (user_id),
    trainer_id  BIGINT      NOT NULL REFERENCES trainer_profile (user_id),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('WORKOUT', 'DIET')),
    content     TEXT        NOT NULL,
    ai_assisted BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_training_plan_member ON training_plan (member_id);

CREATE TABLE notification (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'IN_APP')),
    type    VARCHAR(50) NOT NULL,
    status  VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE consent_record (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    purpose     VARCHAR(30) NOT NULL CHECK (purpose IN ('TERMS', 'HEALTH_DATA', 'MARKETING', 'AI_PROCESSING')),
    granted     BOOLEAN     NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_consent_user_purpose ON consent_record (user_id, purpose, recorded_at DESC);

CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_actor_time ON audit_log (actor_id, created_at DESC);
