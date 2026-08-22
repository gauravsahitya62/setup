-- Planner Projects: event workspace. Adding products/vendors does NOT reserve inventory.
-- Soft-delete via projects.deleted_at. Activity rows are append-only.

CREATE TABLE projects (
    id               UUID PRIMARY KEY,
    planner_id       UUID         NOT NULL,
    name             VARCHAR(200) NOT NULL,
    slug             VARCHAR(220) NOT NULL,
    event_type       VARCHAR(40)  NOT NULL,
    description      TEXT,
    event_date       DATE,
    start_time       TIME,
    end_time         TIME,
    venue_name       VARCHAR(255),
    venue_address    VARCHAR(500),
    city             VARCHAR(120),
    state            VARCHAR(120),
    country          VARCHAR(120),
    guest_count      INTEGER,
    status           VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    cover_image_url  VARCHAR(1000),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_projects_planner FOREIGN KEY (planner_id) REFERENCES users (id),
    CONSTRAINT ck_projects_status CHECK (status IN ('DRAFT', 'PLANNING', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_projects_guest_count CHECK (guest_count IS NULL OR guest_count >= 0),
    CONSTRAINT uq_projects_planner_slug UNIQUE (planner_id, slug)
);

CREATE INDEX idx_projects_planner_id ON projects (planner_id);
CREATE INDEX idx_projects_event_date ON projects (event_date);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_created_at ON projects (created_at);
CREATE INDEX idx_projects_planner_status ON projects (planner_id, status);

CREATE TABLE project_mood_boards (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_mood_boards_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE project_products (
    id                   UUID PRIMARY KEY,
    project_id           UUID NOT NULL,
    product_variant_id   UUID,
    vendor_id            UUID NOT NULL,
    quantity             INTEGER NOT NULL DEFAULT 1,
    notes                TEXT,
    -- Snapshots so vendor price/name changes and hard-deleted variants do not rewrite history.
    product_name         VARCHAR(255) NOT NULL,
    product_slug         VARCHAR(255),
    variant_name         VARCHAR(255) NOT NULL,
    sku                  VARCHAR(100),
    image_url            VARCHAR(1000),
    unit_price_per_day   NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_products_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_products_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT fk_project_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_project_products_quantity CHECK (quantity >= 1),
    CONSTRAINT uq_project_products_variant UNIQUE (project_id, product_variant_id)
);

CREATE INDEX idx_project_products_project_id ON project_products (project_id);
CREATE INDEX idx_project_products_variant_id ON project_products (product_variant_id);
CREATE INDEX idx_project_products_vendor_id ON project_products (vendor_id);
CREATE INDEX idx_project_products_created_at ON project_products (created_at);

CREATE TABLE project_vendors (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL,
    vendor_id   UUID NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_vendors_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_vendors_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT uq_project_vendors UNIQUE (project_id, vendor_id)
);

CREATE INDEX idx_project_vendors_project_id ON project_vendors (project_id);
CREATE INDEX idx_project_vendors_vendor_id ON project_vendors (vendor_id);
CREATE INDEX idx_project_vendors_created_at ON project_vendors (created_at);

CREATE TABLE project_mood_board_items (
    id                      UUID PRIMARY KEY,
    project_mood_board_id   UUID NOT NULL,
    image_url               VARCHAR(1000) NOT NULL,
    source_url              VARCHAR(1000),
    caption                 VARCHAR(500),
    display_order           INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mood_board_items_board FOREIGN KEY (project_mood_board_id) REFERENCES project_mood_boards (id)
);

CREATE INDEX idx_mood_board_items_board_id ON project_mood_board_items (project_mood_board_id);
CREATE INDEX idx_mood_board_items_created_at ON project_mood_board_items (created_at);

CREATE TABLE project_notes (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    pinned      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_notes_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_notes_user FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_project_notes_project_id ON project_notes (project_id);
CREATE INDEX idx_project_notes_created_at ON project_notes (created_at);

CREATE TABLE project_files (
    id            UUID PRIMARY KEY,
    project_id    UUID NOT NULL,
    file_url      VARCHAR(1000) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_type     VARCHAR(150) NOT NULL,
    file_size     BIGINT NOT NULL,
    uploaded_by   UUID NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_files_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_files_user FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT ck_project_files_size CHECK (file_size >= 0)
);

CREATE INDEX idx_project_files_project_id ON project_files (project_id);
CREATE INDEX idx_project_files_created_at ON project_files (created_at);

CREATE TABLE project_activity (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL,
    user_id         UUID,
    activity_type   VARCHAR(50) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    metadata        TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_activity_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_activity_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_project_activity_project_id ON project_activity (project_id);
CREATE INDEX idx_project_activity_created_at ON project_activity (created_at);
CREATE INDEX idx_project_activity_project_created ON project_activity (project_id, created_at DESC);
