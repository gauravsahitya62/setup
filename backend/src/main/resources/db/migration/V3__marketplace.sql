-- Marketplace schema: categories, products, variants, inventory, availability, collections, favorites, tags

CREATE TABLE categories (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(255)    NOT NULL,
    slug                  VARCHAR(255)    NOT NULL UNIQUE,
    description           VARCHAR(1000),
    icon                  VARCHAR(255),
    image_url             VARCHAR(1000),
    parent_category_id    UUID,
    display_order         INTEGER         NOT NULL DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories (id),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'HIDDEN'))
);

CREATE INDEX idx_categories_parent ON categories (parent_category_id);
CREATE INDEX idx_categories_status ON categories (status);
CREATE INDEX idx_categories_display_order ON categories (display_order);

CREATE TABLE products (
    id                     UUID PRIMARY KEY,
    vendor_id              UUID            NOT NULL,
    category_id            UUID            NOT NULL,
    subcategory_id         UUID,
    name                   VARCHAR(255)    NOT NULL,
    slug                   VARCHAR(255)    NOT NULL,
    short_description      VARCHAR(500),
    description            TEXT,
    featured_image         VARCHAR(1000),
    price_per_day          DECIMAL(12, 2)  NOT NULL,
    security_deposit        DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    minimum_rental_days     INTEGER         NOT NULL DEFAULT 1,
    delivery_available      BOOLEAN         NOT NULL DEFAULT FALSE,
    setup_available         BOOLEAN         NOT NULL DEFAULT FALSE,
    pickup_available        BOOLEAN         NOT NULL DEFAULT TRUE,
    status                 VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    featured                BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_subcategory FOREIGN KEY (subcategory_id) REFERENCES categories (id),
    CONSTRAINT uq_products_vendor_slug UNIQUE (vendor_id, slug),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_products_price CHECK (price_per_day >= 0),
    CONSTRAINT ck_products_deposit CHECK (security_deposit >= 0),
    CONSTRAINT ck_products_min_rental_days CHECK (minimum_rental_days >= 1)
);

CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_vendor_id ON products (vendor_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_price_per_day ON products (price_per_day);
CREATE INDEX idx_products_featured ON products (featured);

CREATE TABLE product_variants (
    id                   UUID PRIMARY KEY,
    product_id           UUID            NOT NULL,
    variant_name         VARCHAR(255)    NOT NULL,
    sku                  VARCHAR(100)    NOT NULL UNIQUE,
    color                VARCHAR(100),
    material             VARCHAR(100),
    quantity             INTEGER         NOT NULL DEFAULT 0,
    available_quantity   INTEGER         NOT NULL DEFAULT 0,
    price_per_day        DECIMAL(12, 2),
    status               VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_product_variants_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_product_variants_available CHECK (available_quantity >= 0)
);

CREATE INDEX idx_product_variants_product ON product_variants (product_id);

CREATE TABLE product_images (
    id             UUID PRIMARY KEY,
    product_id     UUID            NOT NULL,
    image_url      VARCHAR(1000)   NOT NULL,
    alt_text       VARCHAR(255),
    display_order  INTEGER         NOT NULL DEFAULT 0,
    is_featured    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_images_product ON product_images (product_id, display_order);

CREATE TABLE product_attributes (
    id                 UUID PRIMARY KEY,
    product_id         UUID            NOT NULL,
    attribute_name      VARCHAR(255)    NOT NULL,
    attribute_value      VARCHAR(1000)   NOT NULL,
    display_order      INTEGER         NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_attributes_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_attributes_product ON product_attributes (product_id, display_order);

CREATE TABLE product_collections (
    id               UUID PRIMARY KEY,
    vendor_id         UUID            NOT NULL,
    name             VARCHAR(255)    NOT NULL,
    slug             VARCHAR(255)    NOT NULL,
    description      VARCHAR(1000),
    featured_image    VARCHAR(1000),
    status           VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_collections_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT uq_product_collections_vendor_slug UNIQUE (vendor_id, slug),
    CONSTRAINT ck_product_collections_status CHECK (status IN ('ACTIVE', 'HIDDEN'))
);

CREATE INDEX idx_product_collections_vendor ON product_collections (vendor_id);

CREATE TABLE product_collection_items (
    id              UUID PRIMARY KEY,
    collection_id    UUID            NOT NULL,
    product_id       UUID            NOT NULL,
    display_order    INTEGER         NOT NULL DEFAULT 0,
    CONSTRAINT fk_collection_items_collection FOREIGN KEY (collection_id) REFERENCES product_collections (id),
    CONSTRAINT fk_collection_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_collection_items UNIQUE (collection_id, product_id)
);

CREATE INDEX idx_collection_items_collection ON product_collection_items (collection_id, display_order);

CREATE TABLE inventory (
    id              UUID PRIMARY KEY,
    variant_id       UUID            NOT NULL UNIQUE,
    total_quantity   INTEGER         NOT NULL DEFAULT 0,
    reserved        INTEGER         NOT NULL DEFAULT 0,
    available       INTEGER         NOT NULL DEFAULT 0,
    maintenance     INTEGER         NOT NULL DEFAULT 0,
    damaged         INTEGER         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_inventory_nonneg CHECK (
        total_quantity >= 0 AND reserved >= 0 AND available >= 0 AND maintenance >= 0 AND damaged >= 0
    )
);

CREATE TABLE availability_calendar (
    id                  UUID PRIMARY KEY,
    variant_id           UUID            NOT NULL,
    date                DATE            NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE',
    quantity_reserved    INTEGER         NOT NULL DEFAULT 0,
    notes               VARCHAR(500),
    CONSTRAINT fk_availability_calendar_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT uq_availability_calendar UNIQUE (variant_id, date),
    CONSTRAINT ck_availability_calendar_status CHECK (
        status IN ('AVAILABLE', 'RESERVED', 'BLOCKED', 'MAINTENANCE')
    )
);

CREATE INDEX idx_availability_calendar_variant ON availability_calendar (variant_id, date);

CREATE TABLE favorite_products (
    id                UUID PRIMARY KEY,
    planner_user_id    UUID            NOT NULL,
    product_id         UUID            NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_favorite_products_user FOREIGN KEY (planner_user_id) REFERENCES users (id),
    CONSTRAINT fk_favorite_products_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_favorite_products UNIQUE (planner_user_id, product_id)
);

CREATE INDEX idx_favorite_products_user ON favorite_products (planner_user_id);

CREATE TABLE product_tags (
    id      UUID PRIMARY KEY,
    name    VARCHAR(100)    NOT NULL,
    slug    VARCHAR(100)    NOT NULL UNIQUE
);

CREATE TABLE product_tag_map (
    product_id  UUID NOT NULL,
    tag_id      UUID NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    CONSTRAINT fk_product_tag_map_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_tag_map_tag FOREIGN KEY (tag_id) REFERENCES product_tags (id)
);

-- Seed baseline categories used across the marketplace UI
INSERT INTO categories (id, name, slug, description, icon, display_order, status) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Furniture', 'furniture', 'Chairs, tables, lounge and event furniture', 'sofa', 1, 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000002', 'Lighting', 'lighting', 'String lights, uplighting, chandeliers and effects', 'lightbulb', 2, 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000003', 'Decor', 'decor', 'Backdrops, centerpieces and styling props', 'sparkles', 3, 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000004', 'Tents & Structures', 'tents-structures', 'Tents, canopies and marquees', 'tent', 4, 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000005', 'Audio & Visual', 'audio-visual', 'Sound systems, projectors and screens', 'speaker', 5, 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000006', 'Tableware', 'tableware', 'Plates, glassware, cutlery and linens', 'utensils', 6, 'ACTIVE');
