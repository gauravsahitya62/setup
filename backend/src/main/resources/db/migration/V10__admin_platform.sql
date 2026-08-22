-- Admin platform: verification workflow, moderation statuses, support, immutable audit logs, typed settings.

ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_last_login_at ON users (last_login_at);

ALTER TABLE vendor_profiles DROP CONSTRAINT ck_vendor_verification;
ALTER TABLE vendor_profiles ADD CONSTRAINT ck_vendor_verification CHECK (
    verification_status IN ('PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'SUSPENDED')
);

ALTER TABLE vendors ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE vendors ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE vendors ADD COLUMN verification_reason VARCHAR(1000);
ALTER TABLE vendors ADD COLUMN verification_reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE vendors ADD COLUMN verification_reviewed_by UUID;
ALTER TABLE vendors ADD COLUMN prior_verification_status VARCHAR(30);
ALTER TABLE vendors ADD COLUMN prior_storefront_status VARCHAR(30);

UPDATE vendors SET verification_status = CASE WHEN verified = TRUE THEN 'VERIFIED' ELSE 'PENDING' END;
UPDATE vendors SET verification_status = 'SUSPENDED' WHERE status = 'SUSPENDED';
UPDATE vendors SET verified = TRUE WHERE verification_status = 'VERIFIED';
UPDATE vendors SET verified = FALSE WHERE verification_status <> 'VERIFIED';

ALTER TABLE vendors ADD CONSTRAINT ck_vendors_verification_status CHECK (
    verification_status IN ('PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'SUSPENDED')
);
ALTER TABLE vendors ADD CONSTRAINT fk_vendors_verification_reviewer FOREIGN KEY (verification_reviewed_by) REFERENCES users (id);

CREATE INDEX idx_vendors_verification_status ON vendors (verification_status);
CREATE INDEX idx_vendors_featured ON vendors (featured);

ALTER TABLE vendor_documents ADD COLUMN review_reason VARCHAR(1000);
ALTER TABLE vendor_documents ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE vendor_documents ADD COLUMN reviewed_by UUID;
ALTER TABLE vendor_documents DROP CONSTRAINT ck_vendor_document_status;
ALTER TABLE vendor_documents ADD CONSTRAINT ck_vendor_document_status CHECK (
    verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'RESUBMISSION_REQUESTED')
);
ALTER TABLE vendor_documents ADD CONSTRAINT fk_vendor_documents_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id);

ALTER TABLE products DROP CONSTRAINT ck_products_status;
ALTER TABLE products ADD CONSTRAINT ck_products_status CHECK (
    status IN ('DRAFT', 'PUBLISHED', 'PENDING_REVIEW', 'REJECTED', 'SUSPENDED', 'ARCHIVED')
);
ALTER TABLE products ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN moderation_reason VARCHAR(1000);

ALTER TABLE categories DROP CONSTRAINT ck_categories_status;
ALTER TABLE categories ADD CONSTRAINT ck_categories_status CHECK (
    status IN ('ACTIVE', 'HIDDEN', 'ARCHIVED')
);
ALTER TABLE categories ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_categories_featured ON categories (featured);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    actor_user_id   UUID,
    action          VARCHAR(80)  NOT NULL,
    resource_type   VARCHAR(40),
    resource_id     UUID,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    metadata        TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_resource_id ON audit_logs (resource_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs (resource_type);

CREATE TABLE support_categories (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL UNIQUE,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    description     VARCHAR(500),
    display_order   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE support_tickets (
    id                  UUID PRIMARY KEY,
    created_by          UUID         NOT NULL,
    subject             VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    category_id         UUID         NOT NULL,
    priority            VARCHAR(20)  NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    assigned_admin_id   UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_support_tickets_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_support_tickets_category FOREIGN KEY (category_id) REFERENCES support_categories (id),
    CONSTRAINT fk_support_tickets_assignee FOREIGN KEY (assigned_admin_id) REFERENCES users (id),
    CONSTRAINT ck_support_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_support_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_USER', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX idx_support_tickets_created_by ON support_tickets (created_by);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);
CREATE INDEX idx_support_tickets_assigned_admin_id ON support_tickets (assigned_admin_id);
CREATE INDEX idx_support_tickets_created_at ON support_tickets (created_at);

CREATE TABLE support_messages (
    id              UUID PRIMARY KEY,
    ticket_id       UUID         NOT NULL,
    author_id       UUID         NOT NULL,
    body            TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_support_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id),
    CONSTRAINT fk_support_messages_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_support_messages_ticket_id ON support_messages (ticket_id);
CREATE INDEX idx_support_messages_created_at ON support_messages (created_at);

CREATE TABLE platform_settings (
    id                              UUID PRIMARY KEY,
    platform_name                   VARCHAR(120) NOT NULL,
    support_email                   VARCHAR(255) NOT NULL,
    default_inventory_threshold     INTEGER      NOT NULL,
    review_edit_window_days         INTEGER      NOT NULL,
    vendor_verification_required    BOOLEAN      NOT NULL,
    max_image_size_bytes            INTEGER      NOT NULL,
    allowed_file_types              VARCHAR(500) NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by                      UUID,
    CONSTRAINT fk_platform_settings_updater FOREIGN KEY (updated_by) REFERENCES users (id)
);

INSERT INTO support_categories (id, name, slug, description, display_order, created_at) VALUES
    ('c1000000-0000-0000-0000-000000000001', 'Account', 'account', 'Login, profile, and access', 10, NOW()),
    ('c1000000-0000-0000-0000-000000000002', 'Bookings', 'bookings', 'Booking requests and reservations', 20, NOW()),
    ('c1000000-0000-0000-0000-000000000003', 'Orders', 'orders', 'Delivery, pickup, and fulfillment', 30, NOW()),
    ('c1000000-0000-0000-0000-000000000004', 'Catalog', 'catalog', 'Products, inventory, and listings', 40, NOW()),
    ('c1000000-0000-0000-0000-000000000005', 'Billing', 'billing', 'Estimates and invoices (no payments in MVP)', 50, NOW()),
    ('c1000000-0000-0000-0000-000000000006', 'Other', 'other', 'Everything else', 60, NOW());

INSERT INTO platform_settings (
    id, platform_name, support_email, default_inventory_threshold, review_edit_window_days,
    vendor_verification_required, max_image_size_bytes, allowed_file_types, updated_at
) VALUES (
    'd1000000-0000-0000-0000-000000000001',
    'FlowSet',
    'support@flowset.local',
    20,
    30,
    FALSE,
    10485760,
    'image/jpeg,image/png,image/webp,image/gif,application/pdf',
    NOW()
);
