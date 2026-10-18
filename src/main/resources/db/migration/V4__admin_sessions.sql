CREATE TABLE admin_sessions (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL REFERENCES admin_users(id),
    csrf_token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT admin_sessions_expiration_ck CHECK (expires_at > created_at),
    CONSTRAINT admin_sessions_revocation_ck CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX admin_sessions_user_expiration_idx
    ON admin_sessions (admin_user_id, expires_at DESC);
