-- =====================================================================
-- FollowUp Saathi - initial schema (version 1)
-- One migration creates every table, foreign key and index. Enum values
-- are stored as VARCHAR on purpose: adding a value later must not require
-- a column-type migration. Audit timestamps are UTC instants (TIMESTAMP(6));
-- follow-up scheduling uses business-local DATE columns.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Registered accounts. Passwords are stored as BCrypt hashes only.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120)  NOT NULL,
    email         VARCHAR(180)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    mobile_number VARCHAR(20)   NULL,
    enabled       TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_enabled ON users (enabled);

-- ---------------------------------------------------------------------
-- The business account behind a user. One user owns exactly one business.
-- ---------------------------------------------------------------------
CREATE TABLE businesses (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id           BIGINT        NOT NULL,
    business_name      VARCHAR(140)  NOT NULL,
    business_category  VARCHAR(80)   NULL,
    preferred_language VARCHAR(5)    NOT NULL,
    time_zone          VARCHAR(40)   NOT NULL,
    working_days       VARCHAR(13)   NOT NULL,
    created_at         TIMESTAMP(6)  NOT NULL,
    updated_at         TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_businesses PRIMARY KEY (id),
    CONSTRAINT uk_businesses_owner UNIQUE (owner_id),
    CONSTRAINT fk_businesses_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
CREATE INDEX idx_businesses_name ON businesses (business_name);

-- ---------------------------------------------------------------------
-- Per-business preferences (reminder slot, working hours).
-- ---------------------------------------------------------------------
CREATE TABLE business_settings (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    business_id         BIGINT        NOT NULL,
    reminder_enabled    TINYINT(1)    NOT NULL DEFAULT 1,
    reminder_time       TIME          NOT NULL,
    working_hours_start TIME          NOT NULL,
    working_hours_end   TIME          NOT NULL,
    created_at          TIMESTAMP(6)  NOT NULL,
    updated_at          TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_business_settings PRIMARY KEY (id),
    CONSTRAINT uk_business_settings_business UNIQUE (business_id),
    CONSTRAINT fk_business_settings_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);

-- ---------------------------------------------------------------------
-- Customer enquiries. Every row belongs to exactly one business.
-- ---------------------------------------------------------------------
CREATE TABLE enquiries (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    business_id         BIGINT        NOT NULL,
    customer_name       VARCHAR(120)  NOT NULL,
    phone_number        VARCHAR(20)   NULL,
    email               VARCHAR(180)  NULL,
    service_name        VARCHAR(140)  NULL,
    source              VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    priority            VARCHAR(10)   NOT NULL,
    next_follow_up_date DATE          NULL,
    notes               TEXT          NULL,
    archived            TINYINT(1)    NOT NULL DEFAULT 0,
    converted_at        TIMESTAMP(6)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL,
    updated_at          TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_enquiries PRIMARY KEY (id),
    CONSTRAINT fk_enquiries_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);
CREATE INDEX idx_enquiries_business ON enquiries (business_id);
CREATE INDEX idx_enquiries_business_status ON enquiries (business_id, status);
CREATE INDEX idx_enquiries_next_follow_up ON enquiries (business_id, next_follow_up_date);
CREATE INDEX idx_enquiries_phone ON enquiries (phone_number);
CREATE INDEX idx_enquiries_customer_name ON enquiries (customer_name);

-- ---------------------------------------------------------------------
-- Follow-up calls: never deleted, completed rows keep their outcome.
-- ---------------------------------------------------------------------
CREATE TABLE follow_ups (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    enquiry_id              BIGINT        NOT NULL,
    scheduled_date          DATE          NOT NULL,
    enquiry_status_snapshot VARCHAR(20)   NOT NULL,
    reason                  VARCHAR(300)  NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    outcome                 TEXT          NULL,
    outcome_type            VARCHAR(20)   NULL,
    completed_at            TIMESTAMP(6)  NULL,
    created_at              TIMESTAMP(6)  NOT NULL,
    updated_at              TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_follow_ups PRIMARY KEY (id),
    CONSTRAINT fk_follow_ups_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries (id)
);
CREATE INDEX idx_follow_ups_enquiry ON follow_ups (enquiry_id);
CREATE INDEX idx_follow_ups_enquiry_status ON follow_ups (enquiry_id, status);
CREATE INDEX idx_follow_ups_scheduled ON follow_ups (scheduled_date);

-- ---------------------------------------------------------------------
-- Enquiry history: one row per real user action, never synthesized.
-- ---------------------------------------------------------------------
CREATE TABLE enquiry_activities (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    enquiry_id    BIGINT        NOT NULL,
    actor_user_id BIGINT        NOT NULL,
    event_type    VARCHAR(30)   NOT NULL,
    description   VARCHAR(400)  NOT NULL,
    created_at    TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_enquiry_activities PRIMARY KEY (id),
    CONSTRAINT fk_activities_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries (id),
    CONSTRAINT fk_activities_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);
CREATE INDEX idx_activities_enquiry ON enquiry_activities (enquiry_id);
CREATE INDEX idx_activities_enquiry_time ON enquiry_activities (enquiry_id, created_at);

-- ---------------------------------------------------------------------
-- Message templates with {{placeholder}} tokens.
-- ---------------------------------------------------------------------
CREATE TABLE message_templates (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    business_id   BIGINT        NOT NULL,
    template_name VARCHAR(80)   NOT NULL,
    message_body  TEXT          NOT NULL,
    is_default    TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_message_templates PRIMARY KEY (id),
    CONSTRAINT fk_templates_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);
CREATE INDEX idx_templates_business ON message_templates (business_id);

-- ---------------------------------------------------------------------
-- Messages the owner actually sent themselves (history only).
-- ---------------------------------------------------------------------
CREATE TABLE message_logs (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    enquiry_id   BIGINT        NOT NULL,
    channel      VARCHAR(10)   NOT NULL,
    message_body TEXT          NOT NULL,
    sent_at      TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_message_logs PRIMARY KEY (id),
    CONSTRAINT fk_message_logs_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries (id)
);
CREATE INDEX idx_message_logs_enquiry ON message_logs (enquiry_id);
CREATE INDEX idx_message_logs_sent_at ON message_logs (sent_at);

-- ---------------------------------------------------------------------
-- Server-side sessions. The browser holds an opaque HttpOnly cookie;
-- this table stores the SHA-256 hash of that token, never the token.
-- ---------------------------------------------------------------------
CREATE TABLE user_sessions (
    session_id   VARCHAR(64)   NOT NULL,
    user_id      BIGINT        NOT NULL,
    created_at   TIMESTAMP(6)  NOT NULL,
    expires_at   TIMESTAMP(6)  NOT NULL,
    last_seen_at TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_user_sessions PRIMARY KEY (session_id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_sessions_user ON user_sessions (user_id);
CREATE INDEX idx_sessions_expires_at ON user_sessions (expires_at);

-- ---------------------------------------------------------------------
-- One-time mobile verification codes (no SMS provider in version one).
-- ---------------------------------------------------------------------
CREATE TABLE mobile_verifications (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    phone_number VARCHAR(15)   NOT NULL,
    code         VARCHAR(6)    NOT NULL,
    expires_at   TIMESTAMP(6)  NOT NULL,
    used         TINYINT(1)    NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_mobile_verifications PRIMARY KEY (id)
);
CREATE INDEX idx_mobile_verifications_phone ON mobile_verifications (phone_number);
CREATE INDEX idx_mobile_verifications_expires_at ON mobile_verifications (expires_at);
