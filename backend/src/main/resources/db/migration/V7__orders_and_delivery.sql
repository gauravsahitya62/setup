-- Orders + delivery/pickup. Created from a CONFIRMED booking request.
-- Inventory remains on inventory_reservations.booking_request_id (V4).
-- Do not duplicate reservation rows from this module.

CREATE TABLE order_number_counters (
    counter_year INTEGER PRIMARY KEY,
    last_value   INTEGER NOT NULL
);

CREATE TABLE orders (
    id                      UUID PRIMARY KEY,
    order_number            VARCHAR(32)  NOT NULL,
    booking_request_id      UUID         NOT NULL,
    project_id              UUID         NOT NULL,
    planner_id              UUID         NOT NULL,
    vendor_id               UUID         NOT NULL,
    status                  VARCHAR(40)  NOT NULL,
    event_date              DATE,
    rental_start_date       DATE         NOT NULL,
    rental_end_date         DATE         NOT NULL,
    delivery_date           DATE,
    pickup_date             DATE,
    estimated_subtotal      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    security_deposit        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    delivery_fee            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency                VARCHAR(3)   NOT NULL DEFAULT 'INR',
    planner_notes           TEXT,
    vendor_notes            TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP WITH TIME ZONE,
    cancelled_at            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT uq_orders_booking_request UNIQUE (booking_request_id),
    CONSTRAINT fk_orders_booking FOREIGN KEY (booking_request_id) REFERENCES booking_requests (id),
    CONSTRAINT fk_orders_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_orders_planner FOREIGN KEY (planner_id) REFERENCES users (id),
    CONSTRAINT fk_orders_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_orders_status CHECK (status IN (
        'PENDING', 'CONFIRMED', 'DELIVERY_SCHEDULED', 'OUT_FOR_DELIVERY', 'DELIVERED',
        'EVENT_COMPLETED', 'PICKUP_SCHEDULED', 'PICKED_UP', 'COMPLETED', 'CANCELLED'
    )),
    CONSTRAINT ck_orders_rental_dates CHECK (rental_end_date >= rental_start_date)
);

CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_orders_booking_request_id ON orders (booking_request_id);
CREATE INDEX idx_orders_project_id ON orders (project_id);
CREATE INDEX idx_orders_planner_id ON orders (planner_id);
CREATE INDEX idx_orders_vendor_id ON orders (vendor_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_delivery_date ON orders (delivery_date);
CREATE INDEX idx_orders_pickup_date ON orders (pickup_date);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_vendor_status ON orders (vendor_id, status);
CREATE INDEX idx_orders_planner_status ON orders (planner_id, status);

CREATE TABLE order_items (
    id                      UUID PRIMARY KEY,
    order_id                UUID         NOT NULL,
    product_variant_id      UUID,
    product_name_snapshot   VARCHAR(255) NOT NULL,
    variant_name_snapshot   VARCHAR(255) NOT NULL,
    sku_snapshot            VARCHAR(100),
    quantity                INTEGER      NOT NULL,
    price_per_day_snapshot  NUMERIC(12, 2) NOT NULL,
    rental_days             INTEGER      NOT NULL,
    subtotal                NUMERIC(14, 2) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT ck_order_items_qty CHECK (quantity >= 1),
    CONSTRAINT ck_order_items_days CHECK (rental_days >= 1)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_variant_id ON order_items (product_variant_id);

CREATE TABLE order_status_history (
    id              UUID PRIMARY KEY,
    order_id        UUID         NOT NULL,
    from_status     VARCHAR(40),
    to_status       VARCHAR(40),
    event_type      VARCHAR(40)  NOT NULL,
    description     VARCHAR(500) NOT NULL,
    note            TEXT,
    actor_id        UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_history_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);

CREATE INDEX idx_order_history_order_id ON order_status_history (order_id);
CREATE INDEX idx_order_history_created_at ON order_status_history (created_at);

CREATE TABLE deliveries (
    id                      UUID PRIMARY KEY,
    order_id                UUID         NOT NULL UNIQUE,
    delivery_date           DATE,
    preferred_time          TIME,
    actual_delivery_time    TIMESTAMP WITH TIME ZONE,
    venue_name              VARCHAR(255),
    venue_address           VARCHAR(500),
    city                    VARCHAR(120),
    state                   VARCHAR(120),
    contact_person          VARCHAR(200),
    contact_phone           VARCHAR(40),
    delivery_notes          TEXT,
    status                  VARCHAR(40)  NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_deliveries_status CHECK (status IN (
        'SCHEDULED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'CANCELLED'
    ))
);

CREATE INDEX idx_deliveries_order_id ON deliveries (order_id);
CREATE INDEX idx_deliveries_status ON deliveries (status);
CREATE INDEX idx_deliveries_date ON deliveries (delivery_date);
CREATE INDEX idx_deliveries_city ON deliveries (city);

CREATE TABLE delivery_status_history (
    id                  UUID PRIMARY KEY,
    delivery_id         UUID         NOT NULL,
    from_status         VARCHAR(40),
    to_status           VARCHAR(40),
    changed_by          UUID,
    note                TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_delivery_history_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id),
    CONSTRAINT fk_delivery_history_actor FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_delivery_history_delivery_id ON delivery_status_history (delivery_id);

CREATE TABLE delivery_proofs (
    id              UUID PRIMARY KEY,
    delivery_id     UUID         NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(120) NOT NULL,
    uploaded_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    uploaded_by     UUID         NOT NULL,
    CONSTRAINT fk_delivery_proofs_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id),
    CONSTRAINT fk_delivery_proofs_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

CREATE INDEX idx_delivery_proofs_delivery_id ON delivery_proofs (delivery_id);

CREATE TABLE pickup_details (
    id                      UUID PRIMARY KEY,
    order_id                UUID         NOT NULL UNIQUE,
    pickup_date             DATE,
    preferred_time          TIME,
    actual_pickup_time      TIMESTAMP WITH TIME ZONE,
    notes                   TEXT,
    status                  VARCHAR(40)  NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pickup_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_pickup_status CHECK (status IN ('SCHEDULED', 'PICKED_UP', 'CANCELLED'))
);

CREATE INDEX idx_pickup_order_id ON pickup_details (order_id);

CREATE TABLE order_notes (
    id              UUID PRIMARY KEY,
    order_id        UUID         NOT NULL,
    author_id       UUID         NOT NULL,
    content         TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_notes_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_notes_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_order_notes_order_id ON order_notes (order_id);
