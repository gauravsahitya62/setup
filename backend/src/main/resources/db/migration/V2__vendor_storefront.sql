-- Vendor storefront schema (extends auth vendor_profiles)

CREATE TABLE vendors (
    id                      UUID PRIMARY KEY,
    owner_user_id           UUID            NOT NULL UNIQUE,
    company_name            VARCHAR(255)    NOT NULL,
    slug                    VARCHAR(255)    NOT NULL UNIQUE,
    description             TEXT,
    short_description       VARCHAR(500),
    gst_number              VARCHAR(50),
    business_type           VARCHAR(100),
    established_year        INTEGER,
    website                 VARCHAR(500),
    email                   VARCHAR(255),
    phone                   VARCHAR(30),
    logo_url                VARCHAR(1000),
    banner_url              VARCHAR(1000),
    rating                  DECIMAL(3, 2)   NOT NULL DEFAULT 0,
    review_count            INTEGER         NOT NULL DEFAULT 0,
    completed_events        INTEGER         NOT NULL DEFAULT 0,
    response_time_minutes   INTEGER,
    verified                BOOLEAN         NOT NULL DEFAULT FALSE,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_vendors_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT ck_vendors_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUSPENDED')),
    CONSTRAINT ck_vendors_rating CHECK (rating >= 0 AND rating <= 5)
);

CREATE INDEX idx_vendors_status ON vendors (status);
CREATE INDEX idx_vendors_verified ON vendors (verified);
CREATE INDEX idx_vendors_business_type ON vendors (business_type);
CREATE INDEX idx_vendors_company_name ON vendors (company_name);

CREATE TABLE vendor_service_areas (
    id          UUID PRIMARY KEY,
    vendor_id   UUID            NOT NULL,
    country     VARCHAR(100)    NOT NULL,
    state       VARCHAR(100)    NOT NULL,
    city        VARCHAR(100)    NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_service_areas_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT uq_vendor_service_area UNIQUE (vendor_id, country, state, city)
);

CREATE INDEX idx_vendor_service_areas_city ON vendor_service_areas (city);
CREATE INDEX idx_vendor_service_areas_state ON vendor_service_areas (state);

CREATE TABLE vendor_portfolio (
    id              UUID PRIMARY KEY,
    vendor_id       UUID            NOT NULL,
    image_url       VARCHAR(1000)   NOT NULL,
    title           VARCHAR(255),
    description     VARCHAR(1000),
    display_order   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_portfolio_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id)
);

CREATE INDEX idx_vendor_portfolio_vendor ON vendor_portfolio (vendor_id, display_order);

CREATE TABLE vendor_documents (
    id                    UUID PRIMARY KEY,
    vendor_id             UUID            NOT NULL,
    file_url              VARCHAR(1000)   NOT NULL,
    file_name             VARCHAR(255)    NOT NULL,
    document_type         VARCHAR(50)     NOT NULL,
    verification_status   VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_documents_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_vendor_document_type CHECK (
        document_type IN ('GST_CERTIFICATE', 'BUSINESS_LICENSE', 'PAN', 'IDENTITY', 'OTHER')
    ),
    CONSTRAINT ck_vendor_document_status CHECK (
        verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')
    )
);

CREATE INDEX idx_vendor_documents_vendor ON vendor_documents (vendor_id);

CREATE TABLE vendor_social_links (
    id          UUID PRIMARY KEY,
    vendor_id   UUID            NOT NULL UNIQUE,
    instagram   VARCHAR(500),
    facebook    VARCHAR(500),
    linkedin    VARCHAR(500),
    youtube     VARCHAR(500),
    pinterest   VARCHAR(500),
    website     VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_social_links_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id)
);

CREATE TABLE vendor_business_hours (
    id          UUID PRIMARY KEY,
    vendor_id   UUID            NOT NULL,
    day_of_week SMALLINT        NOT NULL,
    open_time   TIME,
    close_time  TIME,
    closed      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_business_hours_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT uq_vendor_business_hours UNIQUE (vendor_id, day_of_week),
    CONSTRAINT ck_vendor_day_of_week CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE vendor_team_members (
    id          UUID PRIMARY KEY,
    vendor_id   UUID            NOT NULL,
    user_id     UUID,
    full_name   VARCHAR(200)    NOT NULL,
    role_title  VARCHAR(100),
    email       VARCHAR(255),
    phone       VARCHAR(30),
    avatar_url  VARCHAR(1000),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_team_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_vendor_team_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_vendor_team_vendor ON vendor_team_members (vendor_id);
