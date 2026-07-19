-- FlowSet auth schema (PostgreSQL; H2 runs in PostgreSQL mode for tests)

CREATE TABLE roles (
    id          UUID PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(30),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles (name),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED'))
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);

CREATE TABLE refresh_tokens (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL,
    token_hash       VARCHAR(255) NOT NULL UNIQUE,
    expires_at       TIMESTAMP WITH TIME ZONE  NOT NULL,
    revoked_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    user_agent       VARCHAR(512),
    ip_address       VARCHAR(64),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE  NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE  NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE planner_profiles (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL UNIQUE,
    company_name  VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_planner_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE vendor_profiles (
    id                 UUID PRIMARY KEY,
    user_id            UUID         NOT NULL UNIQUE,
    business_name      VARCHAR(255) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_vendor_verification CHECK (
        verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'SUSPENDED')
    )
);

CREATE TABLE admin_profiles (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL UNIQUE,
    title       VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO roles (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'PLANNER', 'Event planner account'),
    ('22222222-2222-2222-2222-222222222222', 'VENDOR', 'Rental vendor account'),
    ('33333333-3333-3333-3333-333333333333', 'ADMIN', 'Platform administrator');
