-- Booking requests: planner → vendor transaction workflow.
-- Inventory is reserved only on vendor ACCEPT via InventoryAvailabilityService
-- (inventory_reservations.booking_request_id). PENDING requests do not consume stock.

CREATE TABLE booking_requests (
    id                      UUID PRIMARY KEY,
    project_id              UUID         NOT NULL,
    planner_id              UUID         NOT NULL,
    vendor_id               UUID         NOT NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    event_date              DATE,
    rental_start_date       DATE         NOT NULL,
    rental_end_date         DATE         NOT NULL,
    delivery_date           DATE,
    pickup_date             DATE,
    venue_name              VARCHAR(255),
    venue_address           VARCHAR(500),
    city                    VARCHAR(120),
    state                   VARCHAR(120),
    planner_notes           TEXT,
    vendor_notes            TEXT,
    rejection_reason        VARCHAR(40),
    estimated_rental_value  NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency                VARCHAR(3)   NOT NULL DEFAULT 'INR',
    requested_at            TIMESTAMP WITH TIME ZONE,
    responded_at            TIMESTAMP WITH TIME ZONE,
    viewed_at               TIMESTAMP WITH TIME ZONE,
    cancelled_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_requests_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_booking_requests_planner FOREIGN KEY (planner_id) REFERENCES users (id),
    CONSTRAINT fk_booking_requests_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_booking_requests_status CHECK (status IN (
        'DRAFT', 'PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'CONFIRMED', 'COMPLETED'
    )),
    CONSTRAINT ck_booking_requests_rental_dates CHECK (rental_end_date >= rental_start_date)
);

CREATE INDEX idx_booking_requests_planner_id ON booking_requests (planner_id);
CREATE INDEX idx_booking_requests_vendor_id ON booking_requests (vendor_id);
CREATE INDEX idx_booking_requests_project_id ON booking_requests (project_id);
CREATE INDEX idx_booking_requests_status ON booking_requests (status);
CREATE INDEX idx_booking_requests_event_date ON booking_requests (event_date);
CREATE INDEX idx_booking_requests_created_at ON booking_requests (created_at);
CREATE INDEX idx_booking_requests_vendor_status ON booking_requests (vendor_id, status);
CREATE INDEX idx_booking_requests_planner_status ON booking_requests (planner_id, status);

CREATE TABLE booking_request_items (
    id                      UUID PRIMARY KEY,
    booking_request_id      UUID         NOT NULL,
    product_variant_id      UUID,
    product_name_snapshot   VARCHAR(255) NOT NULL,
    variant_name_snapshot   VARCHAR(255) NOT NULL,
    sku_snapshot            VARCHAR(100),
    vendor_id               UUID         NOT NULL,
    quantity                INTEGER      NOT NULL,
    price_per_day_snapshot  NUMERIC(12, 2) NOT NULL,
    rental_start_date       DATE         NOT NULL,
    rental_end_date         DATE         NOT NULL,
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_items_request FOREIGN KEY (booking_request_id) REFERENCES booking_requests (id),
    CONSTRAINT fk_booking_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT fk_booking_items_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_booking_items_qty CHECK (quantity >= 1),
    CONSTRAINT ck_booking_items_dates CHECK (rental_end_date >= rental_start_date)
);

CREATE INDEX idx_booking_items_request_id ON booking_request_items (booking_request_id);
CREATE INDEX idx_booking_items_variant_id ON booking_request_items (product_variant_id);
CREATE INDEX idx_booking_items_vendor_id ON booking_request_items (vendor_id);

CREATE TABLE booking_request_status_history (
    id                  UUID PRIMARY KEY,
    booking_request_id  UUID         NOT NULL,
    from_status         VARCHAR(30),
    to_status           VARCHAR(30),
    event_type          VARCHAR(40)  NOT NULL,
    description         VARCHAR(500) NOT NULL,
    note                TEXT,
    actor_id            UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_history_request FOREIGN KEY (booking_request_id) REFERENCES booking_requests (id),
    CONSTRAINT fk_booking_history_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);

CREATE INDEX idx_booking_history_request_id ON booking_request_status_history (booking_request_id);
CREATE INDEX idx_booking_history_created_at ON booking_request_status_history (created_at);

CREATE TABLE booking_request_notes (
    id                  UUID PRIMARY KEY,
    booking_request_id  UUID         NOT NULL,
    author_id           UUID         NOT NULL,
    content             TEXT         NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_notes_request FOREIGN KEY (booking_request_id) REFERENCES booking_requests (id),
    CONSTRAINT fk_booking_notes_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_booking_notes_request_id ON booking_request_notes (booking_request_id);

CREATE TABLE booking_request_delivery_details (
    id                          UUID PRIMARY KEY,
    booking_request_id          UUID         NOT NULL UNIQUE,
    delivery_date               DATE,
    preferred_delivery_time     TIME,
    venue_name                  VARCHAR(255),
    venue_address               VARCHAR(500),
    city                        VARCHAR(120),
    state                       VARCHAR(120),
    contact_person              VARCHAR(200),
    contact_phone               VARCHAR(40),
    delivery_notes              TEXT,
    pickup_date                 DATE,
    preferred_pickup_time       TIME,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_delivery_request FOREIGN KEY (booking_request_id) REFERENCES booking_requests (id)
);

CREATE INDEX idx_booking_delivery_request_id ON booking_request_delivery_details (booking_request_id);
