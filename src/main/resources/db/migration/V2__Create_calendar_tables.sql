-- Create calendar_tokens table
CREATE TABLE calendar_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('PARENT', 'STUDENT')),
    google_email VARCHAR(255),
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    token_expires_at TIMESTAMP NOT NULL,
    calendar_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id, account_type)
);

-- Create calendar_sync_settings table
CREATE TABLE calendar_sync_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    sync_enabled BOOLEAN DEFAULT TRUE,
    sync_to_parent_calendar BOOLEAN DEFAULT TRUE,
    sync_to_student_calendar BOOLEAN DEFAULT TRUE,
    parent_reminder_minutes VARCHAR(255) DEFAULT '[1440,120]', -- 24h, 2h
    student_reminder_minutes VARCHAR(255) DEFAULT '[120,30]',   -- 2h, 30min
    included_courses TEXT,
    excluded_assignment_types TEXT,
    sync_completed_assignments BOOLEAN DEFAULT FALSE,
    auto_sync_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id)
);

-- Create calendar_event_mappings table
CREATE TABLE calendar_event_mappings (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('PARENT', 'STUDENT')),
    google_event_id VARCHAR(255) NOT NULL,
    google_calendar_id VARCHAR(255) NOT NULL,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(assignment_id, student_id, account_type)
);

-- Create student_calendar_invitations table
CREATE TABLE student_calendar_invitations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    student_email VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id)
);

-- Create indexes for better performance
CREATE INDEX idx_calendar_tokens_user_student ON calendar_tokens(user_id, student_id);
CREATE INDEX idx_calendar_tokens_account_type ON calendar_tokens(account_type);
CREATE INDEX idx_calendar_tokens_expires_at ON calendar_tokens(token_expires_at);

CREATE INDEX idx_sync_settings_user_student ON calendar_sync_settings(user_id, student_id);
CREATE INDEX idx_sync_settings_sync_enabled ON calendar_sync_settings(sync_enabled);
CREATE INDEX idx_sync_settings_auto_sync ON calendar_sync_settings(auto_sync_enabled);

CREATE INDEX idx_event_mappings_assignment ON calendar_event_mappings(assignment_id, student_id, account_type);
CREATE INDEX idx_event_mappings_student_account ON calendar_event_mappings(student_id, account_type);
CREATE INDEX idx_event_mappings_google_event ON calendar_event_mappings(google_event_id);
CREATE INDEX idx_event_mappings_google_calendar ON calendar_event_mappings(google_calendar_id);
CREATE INDEX idx_event_mappings_last_synced ON calendar_event_mappings(last_synced_at);

CREATE INDEX idx_invitations_token ON student_calendar_invitations(invitation_token);
CREATE INDEX idx_invitations_status ON student_calendar_invitations(status, expires_at);
CREATE INDEX idx_invitations_user_student ON student_calendar_invitations(user_id, student_id);
CREATE INDEX idx_invitations_email ON student_calendar_invitations(student_email);